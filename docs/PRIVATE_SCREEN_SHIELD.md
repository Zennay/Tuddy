# Privéweergave tijdens gevoelige gesprekken

Open **Taal- en modelinstellingen → Privacy · privéweergave**. Zet
**Screenshots en schermopname blokkeren** aan als je een gevoelig gesprek
leest, of uit als je expliciet een screenshot nodig hebt.

De app stelt hiervoor Androids bestaande venstervlag
`WindowManager.LayoutParams.FLAG_SECURE` in. Android kan hiermee
screenshots, normale systeemschermopnames en de weergave van Tuddy in
de recente-appsweergave beperken of afschermen. De bescherming geldt
direct na het schakelen en wordt bij het opnieuw openen van Tuddy
weer ingesteld.

Het enige opgeslagen gegeven is één app-private Boolean die aangeeft
of deze instelling aan staat. Tuddy schrijft geen transcriptinhoud,
audio, schermbeelden, persoonsinformatie of gesprekstijdstempels
voor deze functie weg. Aan- en uitzetten veranderen niets aan de
bestaande offline herkenning, tekstvertaling of persoonlijke woorden.

**Beperkingen:** `FLAG_SECURE` is een verzoek aan het Android-systeem.
Apparaatfabrikanten, beheerde toestellen en externe opnameapparatuur
kunnen afwijkend gedrag hebben. Een camera die naar het scherm wijst
wordt niet geblokkeerd. De optie versleutelt of verwijdert geen al
gedeelde transcriptbestanden en is niet hetzelfde als toestemming
voor een opname. Als Android de instelling niet accepteert, wordt
de vorige status behouden en ziet de gebruiker een melding.

Er worden geen aanvullende Android-toestemmingen aangevraagd.
Deze broncodefunctie bewijst geen fysieke toestelacceptatie en
geen vrijgegeven, getekende APK.
