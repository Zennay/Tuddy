# Ondertitelregels op woordgrenzen afbreken

Open een zelfgekozen SRT/WebVTT-document en kies **Bewerk document ▾ → Lange regels afbreken…**. In de preview selecteer je **32, 42 of 50 tekens per regel**. De app telt vóór bevestiging welke cues veranderen, hoeveel extra regelafbrekingen ontstaan en hoeveel regels nog langer blijven.

De formattering:
- vervangt uitsluitend een gewone losse spatie tussen twee tekstgedeelten door een regeleinde;
- behoudt de overige tekst, woorden en tijdcodes; voert geen vertaling of audioanalyse uit;
- breekt geen woorden middenin af, en laat niet-breekbare lange woorden of bewuste dubbele spaties ongemoeid;
- slaat een cue over wanneer de editorgrens van acht tekstregels zou worden overschreden;
- weigert een voorstel wanneer de bestaande maximale grenzen van 500 cues, 256 KiB of veilige SRT/VTT-export niet gehaald worden.

**Pas afbrekingen tijdelijk toe** verandert alleen het werkgeheugenconcept. De bestaande **Terug**, **Opnieuw** en **Herstel origineel** blijven beschikbaar. Alleen na een aparte actie **Bewaar kopie SRT/VTT…** wordt een nieuwe kopie naar een zelfgekozen Android-bestandsprovider geschreven; het geopende bronbestand blijft intact. Een externe provider kan buiten de app synchroniseren.

De nieuwe **Snel lezen**- en **Lange regel**-meldingen uit de tijdlijninspecteur kunnen aanleiding zijn voor handmatig redigeren, maar de formattering corrigeert geen te korte weergaveduur en maakt geen oordeel over inhoud, taal of toegankelijkheid. Er zijn geen nieuwe permissies, netwerkaanroepen, modellen of automatische opslag toegevoegd.
