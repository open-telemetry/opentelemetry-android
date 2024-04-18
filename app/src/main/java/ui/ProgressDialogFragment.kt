@file:Suppress("DEPRECATION")

package ui

import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class ProgressDialogFragment : DialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = ProgressDialog(activity, theme)
        dialog.isIndeterminate = true
        dialog.setMessage("Processing")
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        return dialog
    }

    companion object {
        val TAG: String ="processing"
    }
}