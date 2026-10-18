# Toegankelijkheid: een korte trilling bij een nieuwe spraakzin

In **Taal- en modelinstellingen → Toegankelijkheid** kan de gebruiker
het **Korte trilling bij afgeronde ondertitel**-signaal expliciet
aan- of uitzetten. Standaard staat het uit. Alleen wanneer Android
daadwerkelijk trilhardware rapporteert is de schakelaar actief.
Met **Voel een voorbeeldtrilling** kan de gebruiker zelf één
voorbeeldpuls aanvragen.

Tuddy laat hooguit één puls van 65 milliseconden per drie seconden
door Android uitvoeren wanneer de **bestaande on-device
spraakherkenner een nieuwe afgeronde zin** oplevert. Het voorlopige
laatste woord, handmatig getypte tekst, een herhaalde lokale
modelvertaling, achtergrondactiviteit of een oude recognizer-callback
starten geen trilalarm. De herkenner en zijn sessiegeneraties blijven
ongewijzigd. Er komen geen achtergrondservice, automatische
herhaling, systeemnotificaties of remote calls bij.

De enige opgeslagen voorkeurswaarde is één app-private Boolean.
Er wordt geen transcript, gesproken woord, trillingstijdstip,
audio of persoonsgegeven bewaard. Android kan de fysieke
trilsterkte beperken door systeeminstellingen, DND of
toestelspecifiek beleid. De app claimt dus geen gegarandeerd
gevoeld signaal. Er is alleen Androids normale `VIBRATE`-permissie
toegevoegd; die geeft geen toegang tot microfoondata.

**Belangrijk:** een puls bevestigt uitsluitend dat Android een
herkenningsresultaat heeft geleverd, niet dat de transcriptie
of vertaling juist, actueel of medisch/juridisch betrouwbaar is.
Dit betreft de code; de echte toestel- en releasegate blijft open.
