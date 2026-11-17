package com.example.util

import android.content.Context

/**
 * Foss flavor: no Play services on the device, so there is no
 * billing to talk to — Pro can never unlock here, and the upsell
 * says so. The build links no non-free library.
 */
object ProBillingProvider {
    fun create(context: Context): ProBilling = UnavailableProBilling
}
