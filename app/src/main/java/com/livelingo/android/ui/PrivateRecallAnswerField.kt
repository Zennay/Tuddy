package com.livelingo.android.ui

import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * An explicitly temporary Android IME field for a learner's own recalled
 * phrase. Unlike a general-purpose Compose text field, Android's advisory
 * NO_PERSONALIZED_LEARNING and NO_SUGGESTIONS input flags are sent directly
 * to the active keyboard. This cannot force an untrusted IME to comply.
 *
 * No clipboard interception, voice dictation, storage, networking or scoring.
 * The caller owns the RAM value and clears it on Activity background.
 */
@Composable
internal fun PrivateRecallAnswerField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val maxCharacters = 400
    AndroidView(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        factory = { context ->
            EditText(context).apply {
                // Configuration order matters: some EditText setters rewrite
                // EditorInfo flags. Set privacy and IME behavior last.
                setSingleLine(false)
                maxLines = 3
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
                imeOptions = EditorInfo.IME_ACTION_DONE or
                    EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or
                    EditorInfo.IME_FLAG_NO_EXTRACT_UI
                filters = arrayOf(
                    InputFilter.LengthFilter(maxCharacters),
                    InputFilter { source, start, end, _, _, _ ->
                        // Reject pasted control characters before they ever
                        // appear in the native text buffer.
                        val segment = source.subSequence(start, end).toString()
                        val clean = segment.replace('\n', ' ').replace('\r', ' ')
                            .filterNot { it.isISOControl() }
                        if (segment == clean) null else clean
                    }
                )
                hint = "Typ zelf de vertaling (maximaal 400 tekens)"
                contentDescription = "Mijn tijdelijke vertaalantwoord"
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO
                setText(value)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        text: CharSequence?, start: Int, count: Int, after: Int
                    ) = Unit

                    override fun onTextChanged(
                        text: CharSequence?, start: Int, before: Int, count: Int
                    ) = Unit

                    override fun afterTextChanged(text: Editable?) {
                        val current = text?.toString().orEmpty()
                        onValueChange(current)
                    }
                })
                setOnEditorActionListener { view, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        view.clearFocus()
                        (context.getSystemService(Context.INPUT_METHOD_SERVICE)
                            as? InputMethodManager)?.hideSoftInputFromWindow(
                            view.windowToken, 0
                        )
                        true
                    } else false
                }
            }
        },
        update = { edit ->
            edit.isEnabled = enabled
            edit.setTextColor(textColor)
            if (edit.text.toString() != value) {
                edit.setText(value)
                edit.setSelection(value.length)
            }
        }
    )
}
