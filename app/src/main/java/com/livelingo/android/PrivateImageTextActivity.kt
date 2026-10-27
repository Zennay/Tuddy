package com.livelingo.android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * User-selected image -> bundled Latin OCR -> review -> explicit local
 * translator. No camera permission, photo-library access, OCR downloads,
 * persisted content, or automatic translation.
 *
 * Android's picker/provider may independently offer cloud-backed images.
 * Tuddy never queries the library or sends images to a network endpoint.
 */
internal class PrivateImageTextActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var generation = 0L
    private var decodeJob: Job? = null
    private var deadline: Runnable? = null
    private var busy by mutableStateOf(false)
    private var draft by mutableStateOf("")
    private var textBlocks by mutableStateOf<List<PrivateImageTextBlock>>(emptyList())
    private var selectedBlockIds by mutableStateOf<Set<Int>>(emptySet())
    private var notice by mutableStateOf(
        "Kies zelf een afbeelding. Alleen Latijns schrift wordt met " +
            "een ingebouwd offline OCR-model herkend. Controleer de tekst."
    )

    private val picker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) readSelectedImage(uri)
        else notice = "Geen afbeelding gekozen. Er is niets gelezen of opgeslagen."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Restore visual privacy before showing any recognized text.
        PrivateScreenShield.restore(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Tekst uit afbeelding · lokaal",
                            style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Selecteer bewust één foto of screenshot. Tuddy leest alleen " +
                                "deze afbeelding met ingebouwde OCR, zonder microfoon " +
                                "of automatische vertaling. De systeemkiezer kan " +
                                "ook cloudfoto's van jouw provider aanbieden.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                // Never keep a previous image's text across picks.
                                generation++
                                clearReviewedImage()
                                notice = "Kies een afbeelding; geen OCR tot je die selecteert."
                                picker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        ) { Text("Kies een afbeelding in Android") }
                        if (busy) LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(notice, style = MaterialTheme.typography.bodySmall)
                        if (textBlocks.isNotEmpty()) {
                            Text(
                                "Kies de relevante tekstblokken · ${textBlocks.size} gevonden",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Je kunt meerdere stukken kiezen, tot samen 8.000 tekens. " +
                                    "Je ziet en corrigeert de samengevoegde tekst voordat " +
                                    "de offline vertaler opent. Selectie wijzigen vervangt " +
                                    "eventuele handmatige aanpassingen in het tekstveld.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectAllBlocks() }
                            ) { Text("Selecteer alle passende blokken") }
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { clearBlockSelection() }
                            ) { Text("Wis blokselectie") }
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().height(250.dp)
                            ) {
                                items(textBlocks, key = { it.index }) { block ->
                                    val selected = block.index in selectedBlockIds
                                    val oversized = block.text.length >
                                        PrivateImageTextBlocks.MAX_HANDOFF_CHARS
                                    Column {
                                        androidx.compose.foundation.layout.Row {
                                            Checkbox(
                                                checked = selected,
                                                enabled = !oversized,
                                                onCheckedChange = { checked ->
                                                    toggleBlock(block.index, checked)
                                                }
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "Blok ${block.index + 1} · ${block.text.length} tekens" +
                                                        if (oversized) " · te lang" else "",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                                Text(
                                                    block.text.replace('\n', ' ').take(160) +
                                                        if (block.text.length > 160) "…" else "",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                        if (draft.isNotEmpty()) {
                            Text("Controleer en corrigeer de herkende tekst",
                                style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = draft,
                                onValueChange = {
                                    if (it.length <= MAX_TEXT) {
                                        draft = it
                                        notice = "Handmatig aangepast. Controleer tekst en taalrichting."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 5,
                                label = { Text("Afbeeldingstekst · max. 8.000 tekens") }
                            )
                            Button(
                                enabled = !busy && draft.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { openLocalTranslator() }
                            ) {
                                Text("Controleer taal en vertaal lokaal…")
                            }
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { finish() }
                        ) { Text("Sluiten en tekst wissen") }
                        Text(
                            "De tekst blijft alleen tijdelijk in het geheugen. " +
                                "Zolang je hier bent wordt niets gekopieerd of gedeeld. " +
                                "De vertaler downloadt geen ontbrekende modellen.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    private fun readSelectedImage(uri: Uri) {
        val request = ++generation
        decodeJob?.cancel()
        deadline?.let(handler::removeCallbacks)
        deadline = null
        clearReviewedImage()
        busy = true
        notice = "De gekozen afbeelding wordt lokaal gelezen…"
        val timeout = Runnable {
            if (request == generation) {
                generation++
                busy = false
                clearReviewedImage()
                notice = "OCR duurde langer dan 20 seconden. " +
                    "Kies een andere of kleinere afbeelding."
            }
        }
        deadline = timeout
        if (!handler.postDelayed(timeout, 20_000L)) {
            generation++
            busy = false
            notice = "Android kon de tijdslimiet niet instellen. Geen OCR gestart."
            return
        }
        decodeJob = lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching { decodeBounded(uri) }.getOrNull()
            }
            if (request != generation || isFinishing) {
                bitmap?.recycle()
                return@launch
            }
            if (bitmap == null) {
                clearDeadline()
                busy = false
                notice = "Android kon de gekozen afbeelding niet veilig lezen. " +
                    "Gebruik een ondersteunde lokale afbeelding."
                return@launch
            }
            val recognizer = runCatching {
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            }.getOrNull()
            if (recognizer == null) {
                bitmap.recycle()
                clearDeadline()
                busy = false
                notice = "Het ingebouwde offline OCR-model kon niet worden geopend."
                return@launch
            }
            try {
                recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnCompleteListener { task ->
                        bitmap.recycle()
                        recognizer.close()
                        if (request != generation || isFinishing) return@addOnCompleteListener
                        clearDeadline()
                        busy = false
                        if (!task.isSuccessful) {
                            notice = "Offline OCR kon de afbeelding niet verwerken."
                            return@addOnCompleteListener
                        }
                        val rawBlocks = task.result.textBlocks
                            .map { it.text }
                            .ifEmpty { listOf(task.result.text) }
                        val admitted = PrivateImageTextBlocks.admit(rawBlocks)
                        if (admitted == null) {
                            notice = if (task.result.text.isBlank())
                                "Geen herkenbare Latijnse tekst gevonden. " +
                                    "Probeer een scherpere afbeelding."
                            else "Te veel of ongeldige OCR-tekst om veilig te tonen. " +
                                "Kies een kleinere uitsnede."
                            return@addOnCompleteListener
                        }
                        textBlocks = admitted
                        val allIds = admitted.map { it.index }.toSet()
                        val combined = PrivateImageTextBlocks.assemble(admitted, allIds)
                        if (combined != null) {
                            selectedBlockIds = allIds
                            draft = combined
                            notice = "Tekst lokaal herkend en geselecteerd. Controleer " +
                                "de blokken en pas de tekst aan vóór vertalen."
                        } else {
                            selectedBlockIds = emptySet()
                            draft = ""
                            notice = "Meer dan 8.000 tekens herkend. Kies hieronder " +
                                "zelf de relevante tekstblokken; niets is doorgegeven."
                        }
                    }
            } catch (_: Exception) {
                bitmap.recycle()
                recognizer.close()
                if (request == generation) {
                    clearDeadline()
                    busy = false
                    notice = "Offline OCR kon niet worden gestart."
                }
            }
        }
    }

    private fun decodeBounded(uri: Uri): Bitmap {
        require(uri.scheme == "content") { "Alleen gekozen content-URI's" }
        return ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(contentResolver, uri)
        ) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            require(width in 1..10_000 && height in 1..10_000) {
                "Afbeelding te groot of ongeldig"
            }
            val sample = (maxOf(width, height) + 1599) / 1600
            decoder.setTargetSampleSize(maxOf(1, sample))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    private fun clearReviewedImage() {
        draft = ""
        textBlocks = emptyList()
        selectedBlockIds = emptySet()
    }

    private fun toggleBlock(index: Int, checked: Boolean) {
        val next = if (checked) selectedBlockIds + index else selectedBlockIds - index
        if (next.isEmpty()) {
            selectedBlockIds = emptySet()
            draft = ""
            notice = "Geen blokken geselecteerd. Kies eerst de relevante tekst."
            return
        }
        val assembled = PrivateImageTextBlocks.assemble(textBlocks, next)
        if (assembled == null) {
            notice = "Selectie overschrijdt 8.000 tekens. " +
                "Laat een blok weg of kies een kleinere uitsnede."
            return
        }
        selectedBlockIds = next
        draft = assembled
        notice = "Blokselectie bijgewerkt. Controleer de samengevoegde tekst."
    }

    private fun clearBlockSelection() {
        selectedBlockIds = emptySet()
        draft = ""
        notice = "Blokselectie gewist. De herkende blokken blijven tijdelijk beschikbaar."
    }

    private fun selectAllBlocks() {
        // For long documents admit only an exact contiguous prefix of whole
        // blocks that fits. Never silently truncate a selected block's text.
        val accepted = mutableSetOf<Int>()
        textBlocks.forEach { block ->
            val candidate = accepted + block.index
            if (PrivateImageTextBlocks.assemble(textBlocks, candidate) != null) {
                accepted.add(block.index)
            }
        }
        selectedBlockIds = accepted
        draft = PrivateImageTextBlocks.assemble(textBlocks, accepted).orEmpty()
        notice = if (accepted.isEmpty()) "Geen blok past binnen de 8.000-tekengrens."
            else "${accepted.size} blokken geselecteerd; controleer de tekst vóór vertalen."
    }

    private fun openLocalTranslator() {
        val opaque = PrivateImageTextHandoff.offer(draft) ?: run {
            notice = "Geen geldige tekst om door te geven. Controleer je invoer."
            return
        }
        opaque.setClass(this, QuickTranslateActivity::class.java)
        startActivity(opaque)
        finish()
    }

    private fun clearDeadline() {
        deadline?.let(handler::removeCallbacks)
        deadline = null
    }

    override fun onPause() {
        // Also cover picker/overlay transitions which may not reach onStop.
        // The selected content is never kept in an Android task snapshot.
        generation++
        decodeJob?.cancel()
        decodeJob = null
        clearDeadline()
        busy = false
        clearReviewedImage()
        super.onPause()
    }

    override fun onStop() {
        generation++
        decodeJob?.cancel()
        decodeJob = null
        clearDeadline()
        busy = false
        clearReviewedImage()
        super.onStop()
    }

    override fun onDestroy() {
        generation++
        decodeJob?.cancel()
        clearDeadline()
        clearReviewedImage()
        super.onDestroy()
    }

    companion object {
        private const val MAX_TEXT = 8_000
    }
}
