package com.livelingo.android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    private var sharedImageUri by mutableStateOf<Uri?>(null)
    // Transient camera result held only until the user explicitly approves OCR.
    // Never put the picture into saved state, Intent extras or external storage.
    private var pendingCameraPreview by mutableStateOf<Bitmap?>(null)
    private var draft by mutableStateOf("")
    private var textBlocks by mutableStateOf<List<PrivateImageTextBlock>>(emptyList())
    private var selectedBlockIds by mutableStateOf<Set<Int>>(emptySet())
    // Temporary order is explicit user preference for this one OCR review.
    private var blockOrder by mutableStateOf<List<Int>>(emptyList())
    // Never saved in Bundle, disk, preferences, or an Intent. Cleared on Pause.
    private val reviewHistory = PrivateOcrReviewHistory()
    private var reviewVersion by mutableStateOf(0)
    private var findCorrection by mutableStateOf("")
    private var replaceCorrection by mutableStateOf("")
    private var pendingReplacement by mutableStateOf<PrivateOcrReplacePlan?>(null)
    private var clipboardExpirySeconds by mutableStateOf(0)
    // Only held in process RAM after the user explicitly opens Android's
    // document picker. The image and ordinary OCR draft still clear on pause.
    private var pendingDocumentText: String? = null
    private var documentWriteJob: Job? = null
    private var pendingShareText by mutableStateOf<String?>(null)
    // OCR remains the owner's RAM-only draft. Audio is another explicit
    // action on the reviewed text, not a new sharing or translation route.
    private var readbackSpeaker: LocalCaptionSpeaker? = null
    private var readbackEpoch = 0L
    private var readbackPlaying by mutableStateOf(false)
    private var readbackLanguageTag by mutableStateOf("nl")
    private var readbackCompleted by mutableStateOf(0)
    private var readbackTotal by mutableStateOf(0)
    private var readbackMessage by mutableStateOf("")
    private var notice by mutableStateOf(
        "Kies zelf een afbeelding. Alleen Latijns schrift wordt met " +
            "een ingebouwd offline OCR-model herkend. Controleer de tekst."
    )

    private val saveReviewedDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        val approvedText = pendingDocumentText
        pendingDocumentText = null
        when {
            uri == null ->
                notice = "Opslaan geannuleerd. Er is geen tekstbestand door Tuddy geschreven."
            approvedText == null ->
                notice = "De tijdelijke tekst is verlopen. Er is niets geschreven."
            else -> {
                val payload = PrivateImageTextOutboundPolicy.utf8Bytes(approvedText)
                if (payload == null) {
                    notice = "De tekst voldoet niet meer aan de veilige grens. " +
                        "Er is niets naar Android geschreven."
                } else {
                    // Document providers may be remote or stall. Never block
                    // Compose or the Android main thread on provider I/O.
                    documentWriteJob?.cancel()
                    busy = true
                    notice = "Gecontroleerde tekst bewaren op de gekozen Android-locatie…"
                    documentWriteJob = lifecycleScope.launch {
                        val saved = withContext(Dispatchers.IO) {
                            runCatching {
                                contentResolver.openOutputStream(uri, "w")?.use { output ->
                                    output.write(payload)
                                    output.flush()
                                    true
                                } ?: false
                            }.getOrDefault(false)
                        }
                        busy = false
                        if (!isFinishing && !isDestroyed) {
                            notice = if (saved)
                                "Je gecontroleerde afbeeldingstekst is als UTF-8-tekstbestand " +
                                    "op de zelfgekozen Android-locatie opgeslagen."
                            else "Opslaan is mislukt. Controleer de gekozen Android-bestandslocatie."
                        }
                    }
                }
            }
        }
    }

    private val picker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) readSelectedImage(uri)
        else notice = "Geen afbeelding gekozen. Er is niets gelezen of opgeslagen."
    }

    private val camera = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap == null) {
            notice = "Geen camerafoto ontvangen. Niets herkend of bewaard."
        } else if (bitmap.width !in 1..1600 || bitmap.height !in 1..1600 ||
            runCatching { bitmap.byteCount > MAX_CAMERA_PREVIEW_BYTES }.getOrDefault(true)) {
            notice = "Android gaf een te grote of ongeldige cameravoorvertoning. " +
                "Gebruik de fotokiezer voor de volledige afbeelding."
        } else {
            generation++
            clearReviewedImage()
            sharedImageUri = null
            pendingCameraPreview = bitmap
            notice = "Cameravoorvertoning ontvangen, nog niets herkend. " +
                "Controleer eerst de foto en tik bewust op ‘Herken deze camerafoto’."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clipboardExpirySeconds = PrivateClipboardExpiry.selectedSeconds(this)
        // Restore visual privacy before showing any recognized text.
        PrivateScreenShield.restore(this)
        if (savedInstanceState == null && intent?.action == ACTION_REVIEW_EXTERNAL) {
            val incoming = intent
            val selected = incoming?.data
            val clips = incoming?.clipData
            sharedImageUri = selected?.takeIf {
                it.scheme == "content" &&
                    (incoming.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0 &&
                    clips?.itemCount == 1 && clips.getItemAt(0).uri == it
            }
            notice = if (sharedImageUri != null) {
                "Eén afbeelding ontvangen. Er is nog niets gelezen. " +
                    "Tik bewust op ‘Herken gedeelde afbeelding’ om offline OCR te starten."
            } else {
                "De gedeelde afbeelding had geen veilige tijdelijke leesrechten " +
                    "of bevatte meerdere items. Kies hieronder zelf een afbeelding."
            }
            incoming?.data = null
            incoming?.clipData = null
            incoming?.removeExtra(Intent.EXTRA_STREAM)
            incoming?.removeExtra(Intent.EXTRA_TEXT)
        }
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
                        if (sharedImageUri != null) {
                            OutlinedButton(
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val selected = sharedImageUri
                                    sharedImageUri = null
                                    if (selected != null) readSelectedImage(selected)
                                }
                            ) { Text("Herken gedeelde afbeelding · lokaal") }
                        }
                        OutlinedButton(
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                // The external camera owns its camera permission;
                                // Tuddy receives only a transient thumbnail bitmap.
                                generation++
                                decodeJob?.cancel()
                                clearDeadline()
                                busy = false
                                clearReviewedImage()
                                pendingCameraPreview = null
                                sharedImageUri = null
                                notice = "Maak bewust één foto in Android. " +
                                    "Na terugkeer bekijk je die eerst; OCR start niet automatisch."
                                try {
                                    camera.launch(null)
                                } catch (_: RuntimeException) {
                                    notice = "Android kon geen camera openen. " +
                                        "Gebruik de fotokiezer voor een bestaande foto."
                                }
                            }
                        ) { Text("Maak foto met Android-camera") }
                        pendingCameraPreview?.let { preview ->
                            Text(
                                "Camerafoto controleren · nog niet uitgelezen",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Image(
                                bitmap = preview.asImageBitmap(),
                                contentDescription = "Voorvertoning van de net gemaakte camerafoto",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
                            )
                            Text(
                                "Dit is de kleinere camera-voorvertoning, geen foto op " +
                                    "volledige resolutie. Voor kleine letters kun je " +
                                    "beter een bestaande scherpe foto kiezen.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val image = pendingCameraPreview
                                    pendingCameraPreview = null
                                    if (image != null) readCapturedImage(image)
                                }
                            ) { Text("Herken deze camerafoto · lokaal") }
                            OutlinedButton(
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    pendingCameraPreview = null
                                    notice = "Camerafoto weggegooid zonder OCR."
                                }
                            ) { Text("Verwerp camerafoto") }
                        }
                        OutlinedButton(
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                pendingCameraPreview = null
                                sharedImageUri = null
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
                                "Kies relevante tekstblokken · ${textBlocks.size} gevonden · ${selectedBlockIds.size} gekozen",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Je kunt meerdere stukken kiezen, tot samen 8.000 tekens. " +
                                    "Zet bij een meerkolomsfoto de blokken handmatig in de " +
                                    "juiste leesvolgorde. Controleer de volledige tekst " +
                                    "voordat de offline vertaler opent. Selectie of volgorde " +
                                    "wijzigen vervangt je aanpassingen in het tekstveld.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectAllBlocks() }
                            ) { Text("Vul selectie aan tot maximaal 8.000 tekens") }
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { clearBlockSelection() }
                            ) { Text("Wis blokselectie") }
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = blockOrder != textBlocks.map { it.index },
                                onClick = { restoreBlockOrder() }
                            ) { Text("Herstel oorspronkelijke OCR-volgorde") }
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().height(250.dp)
                            ) {
                                items(
                                    blockOrder.mapNotNull { id ->
                                        textBlocks.firstOrNull { it.index == id }
                                    },
                                    key = { it.index }
                                ) { block ->
                                    val selected = block.index in selectedBlockIds
                                    val orderIndex = blockOrder.indexOf(block.index)
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
                                                    "Positie ${orderIndex + 1} · OCR-blok ${block.index + 1} · " +
                                                        "${block.text.length} tekens" +
                                                        if (oversized) " · te lang" else "",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                                Text(
                                                    block.text.replace('\n', ' ').take(160) +
                                                        if (block.text.length > 160) "…" else "",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                androidx.compose.foundation.layout.Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    OutlinedButton(
                                                        enabled = orderIndex > 0,
                                                        onClick = {
                                                            moveReviewedBlock(block.index, -1)
                                                        }
                                                    ) { Text("↑ Omhoog") }
                                                    OutlinedButton(
                                                        enabled = orderIndex < blockOrder.lastIndex,
                                                        onClick = {
                                                            moveReviewedBlock(block.index, 1)
                                                        }
                                                    ) { Text("↓ Omlaag") }
                                                }
                                            }
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                        if (textBlocks.isNotEmpty()) {
                            // Keep Undo visible even after clearing every block.
                            // This restores the last explicit correction/selection
                            // without re-reading the image or sharing its contents.
                            androidx.compose.foundation.layout.Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    enabled = reviewHistory.canUndo && !busy,
                                    onClick = { reviewHistory.undo()?.let(::restoreReview) }
                                ) { Text("↶ Ongedaan") }
                                OutlinedButton(
                                    enabled = reviewHistory.canRedo && !busy,
                                    onClick = { reviewHistory.redo()?.let(::restoreReview) }
                                ) { Text("↷ Opnieuw") }
                            }
                            Text(
                                "Lokale correctiegeschiedenis · max. 16 stappen · " +
                                    "wissen bij verlaten (stap ${reviewVersion}).",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (draft.isNotEmpty()) {
                            Text("Controleer en corrigeer de herkende tekst",
                                style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = draft,
                                onValueChange = {
                                    if (it.length <= MAX_TEXT) {
                                        applyReview(reviewSnapshot().copy(draft = it), typing = true)
                                        notice = "Handmatig aangepast. Controleer tekst en taalrichting."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 5,
                                label = { Text("Afbeeldingstekst · max. 8.000 tekens") }
                            )
                            OutlinedTextField(
                                value = findCorrection,
                                onValueChange = {
                                    findCorrection = it.take(120)
                                    pendingReplacement = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Zoek letterlijk in gecontroleerde OCR-tekst") }
                            )
                            OutlinedTextField(
                                value = replaceCorrection,
                                onValueChange = {
                                    replaceCorrection = it.take(120)
                                    pendingReplacement = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Vervang met (leeg = verwijderen)") }
                            )
                            OutlinedButton(
                                enabled = !busy && findCorrection.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    pendingReplacement = PrivateOcrReplacePolicy.plan(
                                        draft, findCorrection, replaceCorrection
                                    )
                                    notice = if (pendingReplacement == null)
                                        "Geen geldige letterlijke vervanging: controleer je " +
                                            "zoektekst, maximaal 100 treffers en 8.000 tekens."
                                    else "Bekijk en bevestig de voorgestelde tekstcorrectie."
                                }
                            ) { Text("Toon en bevestig alle vervangingen…") }
                            pendingReplacement?.let { proposal ->
                                AlertDialog(
                                    onDismissRequest = { pendingReplacement = null },
                                    title = { Text("OCR-tekst echt wijzigen?") },
                                    text = {
                                        Text(
                                            "${proposal.occurrences} letterlijke vervangingen. " +
                                                "Nieuwe lengte: ${proposal.updated.length} tekens. " +
                                                "Dit verandert je huidige, beoordeelde tekst, " +
                                                "niet het originele plaatje of OCR-blokken. " +
                                                "Je kunt de wijziging ongedaan maken."
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            if (!busy && draft == proposal.original &&
                                                PrivateOcrReplacePolicy.plan(
                                                    draft, findCorrection, replaceCorrection
                                                ) == proposal) {
                                                applyReview(
                                                    reviewSnapshot().copy(draft = proposal.updated)
                                                )
                                                notice = "${proposal.occurrences} correcties bewust toegepast. " +
                                                    "Controleer de tekst vóór vertaling of delen."
                                            } else {
                                                notice = "Tekst is intussen gewijzigd; bevestig opnieuw."
                                            }
                                            pendingReplacement = null
                                        }) { Text("Pas bewust toe") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { pendingReplacement = null }) {
                                            Text("Annuleren")
                                        }
                                    }
                                )
                            }
                            Button(
                                enabled = !busy && draft.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { openLocalTranslator() }
                            ) {
                                Text("Controleer taal en vertaal lokaal…")
                            }
                            PrivateImageTextReadbackPanel(
                                canRead = !busy && !isFinishing &&
                                    PrivateImageTextReadbackPolicy.prepare(draft) != null,
                                playing = readbackPlaying,
                                languageTag = readbackLanguageTag,
                                completed = readbackCompleted,
                                total = readbackTotal,
                                message = readbackMessage,
                                onLanguageChange = { code ->
                                    stopReadback("Uitspraaktaal gewijzigd. Tik zelf op voorlezen.")
                                    readbackLanguageTag = code
                                },
                                onPlay = { pace -> startReadback(pace) },
                                onStop = {
                                    stopReadback("Lokaal voorlezen bewust gestopt.")
                                }
                            )
                            PrivateImageTextOutboundPanel(
                                enabled = !busy &&
                                    PrivateImageTextOutboundPolicy.reviewedText(draft) != null,
                                expirySeconds = clipboardExpirySeconds,
                                onCopy = { copyReviewedText() },
                                onSave = { saveReviewedText() },
                                onShare = { requestShareReviewedText() },
                                onExpiryChange = { seconds ->
                                    val changed = PrivateClipboardExpiry.changeSeconds(
                                        this@PrivateImageTextActivity, seconds
                                    )
                                    if (changed) clipboardExpirySeconds = seconds
                                    notice = if (changed)
                                        "Klembordinstelling bijgewerkt. " +
                                            "Wissen werkt alleen best-effort op eigen ongewijzigde clips."
                                    else "De klembordinstelling kon niet worden bewaard."
                                }
                            )
                        }
                        pendingShareText?.let { reviewed ->
                            AlertDialog(
                                onDismissRequest = { pendingShareText = null },
                                title = { Text("Afbeeldingstekst buiten Tuddy delen?") },
                                text = {
                                    Text(
                                        "Je stuurt alleen de door jou gecontroleerde OCR-tekst " +
                                            "naar het Android-deelmenu. De ontvangende app kan " +
                                            "de tekst bewaren, uploaden of synchroniseren. " +
                                            "Tuddy verstuurt niets zonder deze bevestiging."
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        pendingShareText = null
                                        if (PrivateImageTextOutboundPolicy.reviewedText(draft) !=
                                            reviewed) {
                                            notice = "De tekst is gewijzigd. Delen geannuleerd."
                                        } else {
                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, reviewed)
                                            }
                                            runCatching {
                                                startActivity(Intent.createChooser(
                                                    send, "Deel gecontroleerde afbeeldingstekst…"
                                                ))
                                            }.onFailure {
                                                notice = "Android kon het deelmenu niet openen. " +
                                                    "Er is niets door Tuddy verzonden."
                                            }
                                        }
                                    }) { Text("Open Android-deelmenu") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { pendingShareText = null }) {
                                        Text("Niet delen")
                                    }
                                }
                            )
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { finish() }
                        ) { Text("Sluiten en tekst wissen") }
                        Text(
                            "De tekst blijft alleen tijdelijk in het geheugen. " +
                                "OCR alleen start geen kopie, export of share. " +
                                "Bij bewuste kopie, opslag of delen verlaat tekst Tuddy; " +
                                "de vertaler downloadt geen ontbrekende modellen.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    private fun readSelectedImage(uri: Uri) = processSelectedImage(uri = uri)

    private fun readCapturedImage(bitmap: Bitmap) =
        processSelectedImage(cameraBitmap = bitmap)

    private fun processSelectedImage(uri: Uri? = null, cameraBitmap: Bitmap? = null) {
        require((uri == null) != (cameraBitmap == null)) {
            "Exactly one explicitly chosen source"
        }
        val request = ++generation
        decodeJob?.cancel()
        deadline?.let(handler::removeCallbacks)
        deadline = null
        clearReviewedImage()
        // A previous ML Kit request may still own a bitmap/recognizer after
        // its visible timeout or Activity pause. Never queue more heavy OCR.
        if (LocalMemoryPressurePolicy.isSystemLowMemory(this)) {
            busy = false
            notice = "Android meldt weinig werkgeheugen. Geen nieuwe afbeelding " +
                "verwerkt; sluit andere zware apps en probeer later opnieuw."
            return
        }
        val ocrLease = LocalOcrWorkBudget.shared.acquire()
        if (ocrLease == null) {
            busy = false
            notice = "De vorige offline afbeeldingherkenning draait nog op dit toestel. " +
                "Wacht tot Android deze werkelijk afrondt voordat je opnieuw herkent. " +
                "Geen tweede OCR-taak gestart."
            return
        }
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
            ocrLease.release()
            busy = false
            notice = "Android kon de tijdslimiet niet instellen. Geen OCR gestart."
            return
        }
        decodeJob = lifecycleScope.launch {
            // The lease stays owned by ML Kit even after the Activity is
            // paused or a visual deadline expires. A coroutine cancellation
            // releases it only if provider work was NOT submitted.
            var providerSubmitted = false
            try {
            val bitmap = if (cameraBitmap != null) cameraBitmap
                else withContext(Dispatchers.IO) {
                    runCatching { decodeBounded(requireNotNull(uri)) }.getOrNull()
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
                val operation = recognizer.process(InputImage.fromBitmap(bitmap, 0))
                providerSubmitted = true
                operation.addOnCompleteListener { task ->
                    try {
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
                        blockOrder = admitted.map { it.index }
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
                        reviewHistory.reset(reviewSnapshot())
                        reviewVersion++
                    } finally {
                        // Task actually completed, even if the owner has been
                        // destroyed or the result callback throws.
                        ocrLease.release()
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
            } finally {
                if (!providerSubmitted) ocrLease.release()
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

    private fun reviewSnapshot(): PrivateOcrReviewState =
        PrivateOcrReviewState(draft, selectedBlockIds, blockOrder)

    private fun applyReview(next: PrivateOcrReviewState, typing: Boolean = false) {
        if (reviewHistory.change(next, typing)) restoreReview(next)
    }

    private fun restoreReview(state: PrivateOcrReviewState) {
        stopReadback("Voorlezen gestopt: gecontroleerde tekst gewijzigd.")
        draft = state.draft
        selectedBlockIds = state.selectedIds
        blockOrder = state.order
        pendingReplacement = null
        reviewVersion++
    }

    private fun clearReviewedImage() {
        stopReadback("Voorlezen gestopt: afbeeldingstekst gewist.")
        draft = ""
        textBlocks = emptyList()
        selectedBlockIds = emptySet()
        blockOrder = emptyList()
        reviewHistory.reset()
        reviewVersion++
        pendingReplacement = null
        findCorrection = ""
        replaceCorrection = ""
    }

    private fun toggleBlock(index: Int, checked: Boolean) {
        stopReadback("Voorlezen gestopt: tekstblokselectie of volgorde gewijzigd.")
        val next = if (checked) selectedBlockIds + index else selectedBlockIds - index
        if (next.isEmpty()) {
            applyReview(reviewSnapshot().copy(selectedIds = emptySet(), draft = ""))
            notice = "Geen blokken geselecteerd. Kies eerst de relevante tekst."
            return
        }
        val assembled = PrivateImageTextBlocks.assemble(textBlocks, next, blockOrder)
        if (assembled == null) {
            notice = "Selectie overschrijdt 8.000 tekens. " +
                "Laat een blok weg of kies een kleinere uitsnede."
            return
        }
        applyReview(reviewSnapshot().copy(selectedIds = next, draft = assembled))
        notice = "Blokselectie bijgewerkt. Controleer de samengevoegde tekst."
    }

    private fun clearBlockSelection() {
        stopReadback("Voorlezen gestopt: tekstblokselectie of volgorde gewijzigd.")
        applyReview(reviewSnapshot().copy(selectedIds = emptySet(), draft = ""))
        notice = "Blokselectie gewist. De herkende blokken blijven tijdelijk beschikbaar."
    }

    private fun selectAllBlocks() {
        stopReadback("Voorlezen gestopt: tekstblokselectie of volgorde gewijzigd.")
        // Include whole blocks in OCR order while staying under
        // 8,000 characters. Oversized blocks are skipped, never truncated.
        // Preserve the user's deliberate block choices. Only append other
        // whole blocks that fit, rather than replacing their selection.
        val accepted = selectedBlockIds.toMutableSet()
        blockOrder.mapNotNull { id ->
            textBlocks.firstOrNull { it.index == id }
        }.forEach { block ->
            val candidate = accepted + block.index
            if (PrivateImageTextBlocks.assemble(textBlocks, candidate, blockOrder) != null) {
                accepted.add(block.index)
            }
        }
        applyReview(
            reviewSnapshot().copy(
                selectedIds = accepted,
                draft = PrivateImageTextBlocks.assemble(textBlocks, accepted, blockOrder).orEmpty()
            )
        )
        notice = if (accepted.isEmpty()) "Geen blok past binnen de 8.000-tekengrens."
            else "${accepted.size} van ${textBlocks.size} blokken geselecteerd. " +
                "Mogelijk zijn blokken overgeslagen; controleer wat is meegenomen."
    }

    /** No content export or model work until user has reviewed this order. */
    private fun moveReviewedBlock(id: Int, direction: Int) {
        stopReadback("Voorlezen gestopt: tekstblokselectie of volgorde gewijzigd.")
        val next = PrivateImageTextBlocks.move(textBlocks, blockOrder, id, direction)
            ?: return
        // Preserve exactly the chosen blocks; do not resurrect unselected ones.
        applyReview(reviewSnapshot().copy(
            order = next,
            draft = PrivateImageTextBlocks.assemble(textBlocks, selectedBlockIds, next).orEmpty()
        ))
        notice = "Leesvolgorde aangepast. Controleer de samengevoegde tekst opnieuw; " +
            "handmatige tekstcorrecties zijn opnieuw samengesteld."
    }

    private fun restoreBlockOrder() {
        stopReadback("Voorlezen gestopt: tekstblokselectie of volgorde gewijzigd.")
        val originalOrder = textBlocks.map { it.index }
        applyReview(reviewSnapshot().copy(
            order = originalOrder,
            draft = PrivateImageTextBlocks.assemble(
                textBlocks, selectedBlockIds, originalOrder
            ).orEmpty()
        ))
        notice = "Oorspronkelijke OCR-volgorde hersteld. " +
            "Controleer de tekst opnieuw vóór vertalen."
    }

    private fun stopReadback(reason: String = "") {
        // Generation guard applies even to delayed TTS initialization or
        // utterance callbacks after a new image, edit, Stop or background.
        readbackEpoch++
        readbackSpeaker?.stop()
        readbackPlaying = false
        readbackCompleted = 0
        readbackTotal = 0
        readbackMessage = reason
    }

    private fun startReadback(pace: LocalSpeechPace) {
        if (busy || isFinishing || isDestroyed) return
        stopReadback()
        val snapshot = draft
        val chunks = PrivateImageTextReadbackPolicy.prepare(snapshot)
        if (chunks == null) {
            readbackMessage = "Selecteer en controleer maximaal 2.400 tekens " +
                "met gewone woorden. Langere tekst kun je eerst inkorten; " +
                "Tuddy leest nooit stilzwijgend slechts een deel voor."
            return
        }
        val language = readbackLanguageTag
        val epoch = readbackEpoch
        readbackPlaying = true
        readbackTotal = chunks.size
        readbackCompleted = 0
        readbackMessage = "Geïnstalleerde lokale Android-stem controleren…"
        val speaker = readbackSpeaker ?: LocalCaptionSpeaker(this).also {
            readbackSpeaker = it
        }
        fun current(): Boolean =
            readbackEpoch == epoch && draft == snapshot && !isFinishing && !isDestroyed
        speaker.speakSequence(
            lines = chunks,
            languageTag = language,
            pace = pace,
            onProgress = { done, total ->
                if (current()) {
                    readbackCompleted = done
                    readbackTotal = total
                }
            },
            feedback = { message ->
                if (current()) readbackMessage = message
            },
            onFinished = { completed ->
                if (current()) {
                    readbackPlaying = false
                    if (completed) {
                        readbackCompleted = chunks.size
                        readbackMessage = "Gecontroleerde afbeeldingstekst lokaal voorgelezen."
                    }
                }
            }
        )
    }

    private fun copyReviewedText() {
        val text = PrivateImageTextOutboundPolicy.reviewedText(draft)
        if (busy || isFinishing || text == null) {
            notice = "Controleer eerst een geldig tekstfragment; er is niets gekopieerd."
            return
        }
        val copied = PrivateClipboardExpiry.copy(this, "gecontroleerde afbeeldingstekst", text)
        notice = if (copied)
            "Goedgekeurde afbeeldingstekst gekopieerd. Andere Android-apps kunnen " +
                "je klembord lezen; automatisch wissen is nooit gegarandeerd."
        else "Android kon de tekst niet veilig kopiëren."
    }

    private fun saveReviewedText() {
        val text = PrivateImageTextOutboundPolicy.reviewedText(draft)
        if (busy || isFinishing || text == null || pendingDocumentText != null) {
            notice = "Controleer eerst de afbeeldingstekst; er is niets opgeslagen."
            return
        }
        pendingDocumentText = text
        try {
            saveReviewedDocument.launch(PrivateImageTextOutboundPolicy.FILE_NAME)
        } catch (_: RuntimeException) {
            pendingDocumentText = null
            notice = "Android kon de bestandskiezer niet openen. Er is niets opgeslagen."
        }
    }

    private fun requestShareReviewedText() {
        val text = PrivateImageTextOutboundPolicy.reviewedText(draft)
        if (busy || isFinishing || text == null) {
            notice = "Controleer de herkende tekst voordat je deze deelt."
            return
        }
        pendingShareText = text
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
        stopReadback()
        // Also cover picker/overlay transitions which may not reach onStop.
        // The selected content is never kept in an Android task snapshot.
        generation++
        decodeJob?.cancel()
        decodeJob = null
        clearDeadline()
        busy = false
        clearReviewedImage()
        pendingCameraPreview = null
        pendingShareText = null
        sharedImageUri = null
        super.onPause()
    }

    override fun onStop() {
        stopReadback()
        generation++
        decodeJob?.cancel()
        decodeJob = null
        clearDeadline()
        busy = false
        clearReviewedImage()
        pendingCameraPreview = null
        sharedImageUri = null
        super.onStop()
    }

    override fun onDestroy() {
        stopReadback()
        readbackSpeaker?.close()
        readbackSpeaker = null
        documentWriteJob?.cancel()
        documentWriteJob = null
        pendingDocumentText = null
        pendingShareText = null
        generation++
        decodeJob?.cancel()
        clearDeadline()
        clearReviewedImage()
        pendingCameraPreview = null
        sharedImageUri = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_REVIEW_EXTERNAL =
            "com.livelingo.android.action.REVIEW_IMAGE_SHARE"
        private const val MAX_TEXT = 8_000
        private const val MAX_CAMERA_PREVIEW_BYTES = 12_000_000
    }
}
