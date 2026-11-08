#!/usr/bin/env python3
"""Emulator smoke driver for Convert264.

Drives the real UI via `adb shell uiautomator dump` — matches on
text / content-desc, taps node bounds centers, never hard-codes
tap coordinates. Polls for controls (first frames can dump empty).

Flow: launch -> "Try Sample" -> wait for SOURCE VIDEO -> trim end
to 2s with the END TIME "-1s" chip -> Convert -> poll progress ->
require "Conversion Complete!" + Open/Share + a real _h264.mp4
output file on disk.

Writes screenshots + smoke.log into tool/smoke-screenshots/.
Exits 0 only if every gate passes.
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "dev.dzianisv.convert264"
SHOT_DIR = "tool/smoke-screenshots"
LOG_PATH = os.path.join(SHOT_DIR, "smoke.log")

failures = []
progress_seen = []
progress_bar_seen = False


def log(msg):
    line = f"[{time.strftime('%H:%M:%S')}] {msg}"
    print(line, flush=True)
    with open(LOG_PATH, "a") as f:
        f.write(line + "\n")


def adb(*args, timeout=120, binary=False):
    r = subprocess.run(["adb"] + list(args), capture_output=True, timeout=timeout)
    if binary:
        return r
    return r.stdout.decode(errors="replace"), r.stderr.decode(errors="replace"), r.returncode


def screenshot(name):
    path = os.path.join(SHOT_DIR, name)
    r = adb("exec-out", "screencap", "-p", binary=True)
    with open(path, "wb") as f:
        f.write(r.stdout)
    log(f"screenshot: {name} ({len(r.stdout)} bytes)")
    return path


def dump_nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/window_dump.xml")
    out, _, _ = adb("pull", "/sdcard/window_dump.xml", "/tmp/window_dump.xml")
    try:
        root = ET.parse("/tmp/window_dump.xml").getroot()
    except Exception as e:
        log(f"dump parse failed: {e}")
        return []
    nodes = []
    for n in root.iter("node"):
        bounds = n.get("bounds", "")
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        cx = cy = 0
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
        nodes.append({
            "text": n.get("text", ""),
            "desc": n.get("content-desc", ""),
            "res": n.get("resource-id", ""),
            "class": n.get("class", ""),
            "clickable": n.get("clickable") == "true",
            "cx": cx, "cy": cy,
            "bounds": bounds,
        })
    return nodes


def screen_text(nodes):
    return " | ".join(n["text"] for n in nodes if n["text"])


def find(nodes, text=None, desc=None, regex=None, res=None):
    for n in nodes:
        if text is not None and n["text"] == text:
            return n
        if desc is not None and n["desc"] == desc:
            return n
        if res is not None and res in n["res"]:
            return n
        if regex is not None and re.search(regex, n["text"]):
            return n
    return None


def tap(node, label):
    log(f"tap: {label} at ({node['cx']},{node['cy']}) text={node['text']!r} desc={node['desc']!r}")
    adb("shell", "input", "tap", str(node["cx"]), str(node["cy"]))
    time.sleep(1)


def wait_for(pred, timeout, label, poll=1.0):
    deadline = time.time() + timeout
    last = []
    while time.time() < deadline:
        nodes = dump_nodes()
        last = nodes
        if pred(nodes):
            return nodes
        if any("Failed" in n["text"] or "failed" in n["text"].lower() for n in nodes
               if n["text"] and "Conversion" in n["text"]):
            pass
        time.sleep(poll)
    return None


def swipe_up():
    out, _, _ = adb("shell", "wm", "size")
    m = re.search(r"(\d+)x(\d+)", out)
    w, h = (1080, 2400) if not m else (int(m.group(1)), int(m.group(2)))
    adb("shell", "input", "swipe", str(w // 2), str(int(h * 0.75)), str(w // 2), str(int(h * 0.25)), "300")
    time.sleep(1)


def scroll_to(text, tries=6):
    for _ in range(tries):
        nodes = dump_nodes()
        n = find(nodes, text=text)
        if n:
            return n
        swipe_up()
    return None


def check_error(nodes):
    for n in nodes:
        t = n["text"]
        if t and ("conversion_error_card" in n["res"] or
                 re.search(r"(transcod\w* failed|failed to|error code|cannot start|no .* encoder)", t, re.I)):
            return t
    return None


def main():
    os.makedirs(SHOT_DIR, exist_ok=True)
    open(LOG_PATH, "w").close()
    log("=== Convert264 emulator smoke test ===")

    # 1. Launch
    nodes = wait_for(lambda ns: find(ns, text="Convert264") is not None, 90, "app title")
    if nodes is None:
        log("FAIL: app title 'Convert264' never appeared")
        screenshot("99-failure.png")
        return 1
    log("app launched, title visible")
    screenshot("01-launch.png")

    # 2. Try Sample Clip
    nodes = wait_for(lambda ns: find(ns, text="Try Sample") is not None, 60, "Try Sample button")
    if nodes is None:
        log("FAIL: 'Try Sample' button never appeared")
        screenshot("99-failure.png")
        return 1
    tap(find(nodes, text="Try Sample"), "Try Sample")
    nodes = wait_for(lambda ns: find(ns, text="SOURCE VIDEO") is not None, 120, "sample loaded")
    if nodes is None:
        log("FAIL: sample clip never loaded (no 'SOURCE VIDEO' within 120s)")
        screenshot("99-failure.png")
        return 1
    dur = [n["text"] for n in nodes if n["text"].startswith("Duration:")]
    log(f"sample loaded; {dur}")
    nodes = wait_for(lambda ns: find(ns, text="Convert to H.264 MP4") is not None, 30, "convert button")
    if nodes is None:
        log("FAIL: 'Convert to H.264 MP4' button never appeared")
        screenshot("99-failure.png")
        return 1
    screenshot("02-sample-loaded.png")

    # 3. Trim: END TIME -1s chip (right half of screen) -> 3s clip becomes 2s
    n = scroll_to("TRIM VIDEO")
    if n is None:
        log("FAIL: 'TRIM VIDEO' card never appeared")
        screenshot("99-failure.png")
        return 1
    nodes = dump_nodes()
    minus = [x for x in nodes if x["text"] == "-1s"]
    # screen width from wm size
    out, _, _ = adb("shell", "wm", "size")
    m = re.search(r"(\d+)x(\d+)", out)
    width = int(m.group(1)) if m else 1080
    right = [x for x in minus if x["cx"] > width // 2]
    if not right:
        log(f"FAIL: no END TIME '-1s' chip found (found {len(minus)} '-1s' nodes)")
        screenshot("99-failure.png")
        return 1
    tap(right[0], "END TIME -1s chip")
    time.sleep(1)
    nodes = dump_nodes()
    txt = screen_text(nodes)
    if "00:02" not in txt:
        log(f"FAIL: trim did not apply — '00:02' not on screen. Visible text: {txt[:800]}")
        screenshot("99-failure.png")
        return 1
    log("trim applied: end time is now 00:02 (was 00:03)")
    screenshot("03-trimmed.png")

    # 4. Convert
    n = scroll_to("Convert to H.264 MP4")
    if n is None:
        log("FAIL: convert button scrolled out of reach")
        screenshot("99-failure.png")
        return 1
    tap(n, "Convert to H.264 MP4")

    # 5. Poll progress
    global progress_bar_seen
    deadline = time.time() + 180
    converting_seen = False
    shot_taken = False
    while time.time() < deadline:
        nodes = dump_nodes()
        err = check_error(nodes)
        if err:
            log(f"FAIL: conversion error on screen: {err}")
            screenshot("99-failure.png")
            return 1
        if find(nodes, text="CONVERTING TO H.264...") is not None:
            converting_seen = True
        if find(nodes, res="conversion_linear_progress") is not None:
            progress_bar_seen = True
        for nd in nodes:
            mm = re.fullmatch(r"(\d+)%", nd["text"].strip())
            if mm:
                p = int(mm.group(1))
                if not progress_seen or progress_seen[-1] != p:
                    progress_seen.append(p)
                    log(f"progress: {p}%")
        if progress_seen and 0 < progress_seen[-1] < 100 and not shot_taken:
            screenshot("04-converting.png")
            shot_taken = True
        if find(nodes, text="Conversion Complete!") is not None:
            break
        time.sleep(1)
    else:
        log(f"FAIL: conversion did not complete within 180s. progress seen: {progress_seen}")
        screenshot("99-failure.png")
        return 1

    log(f"converting screen seen: {converting_seen}, progress bar node seen: {progress_bar_seen}")
    log(f"progress values observed: {progress_seen}")
    intermediate = [p for p in progress_seen if 0 < p < 100]
    if not converting_seen:
        log("FAIL: converting state was never shown")
        return 1
    if not intermediate and not progress_bar_seen:
        log("FAIL: progress bar never observed advancing (no intermediate % and no progress node)")
        return 1
    if not intermediate:
        log("WARN: no intermediate % captured between polls, but progress-bar node was present")

    # 6. Success screen
    nodes = dump_nodes()
    if find(nodes, text="Open") is None or find(nodes, text="Share") is None:
        log("FAIL: success screen missing Open/Share buttons")
        screenshot("99-failure.png")
        return 1
    log("success screen shown with Open + Share")
    dur_nodes = [n["text"] for n in nodes if n["text"]]
    log(f"success screen text: {' | '.join(dur_nodes)[:600]}")
    screenshot("05-success.png")

    log("=== UI FLOW PASSED (output-file checks run in smoke-test.sh) ===")
    log(f"SUMMARY progress={progress_seen} converting={converting_seen} bar={progress_bar_seen}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
