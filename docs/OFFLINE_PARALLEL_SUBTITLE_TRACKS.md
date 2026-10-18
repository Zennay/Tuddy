# Twee offline ondertitelsporen samen lezen

Open een eigen SRT- of WebVTT-document via de bestaande lokale bestandskiezer. Kies daarna **Tweede SRT/VTT-document toevoegen…** en wijs zelf een tweede bestand aan. In het bevestigingsvenster staan nu drie opties:

- **Aansluiten na laatste regel:** tweede document begint een seconde na het eerste.
- **Originele tijdcodes naast elkaar:** alle originele regels chronologisch samen tonen.
- **Combineer twee teksten bij overlappende tijdcodes:** twee door de gebruiker aangeleverde tekstsporen op basis van hun tijdcode-overlap naast elkaar plaatsen.

## Wat de nieuwe koppeling doet

De app koppelt een regel uit het tweede spoor aan hoogstens één nog ongebruikte regel in het eerste spoor, uitsluitend wanneer hun tijdvakken elkaar ten minste de helft van de kortste regellengte overlappen. Van de geldige kandidaten wordt de grootste overlap gekozen. De tijdcodes van het **eerste** spoor blijven voor gekoppelde regels ongewijzigd; de oorspronkelijke tekst staat bovenaan en de tekst uit spoor twee op de volgende regel. Identieke teksten worden niet gedupliceerd.

Optioneel kun je vooraf **alleen het tweede spoor** een vaste verschuiving van -10, -5, -2, 0, +2, +5 of +10 seconden geven. Het is een handmatige keuze voor bestanden met een bekend beginverschil, geen automatisch berekende synchronisatie. Als een regel vóór 00:00 of voorbij de maximale tijd valt, wordt het voorstel geweigerd. De preview herberekent koppelingen bij iedere keuze.

Regels uit het tweede spoor zonder voldoende overlap blijven afzonderlijk op hun eigen tijdcodes zichtbaar, niet stilzwijgend verwijderd. De preview toont exact het aantal koppelingen, niet-gekoppelde regels en het aantal uiteindelijke regels. De uitkomst is **geen** automatische vertaling, taaldetectie, audio-uitlijning of bewijs dat twee zinnen werkelijk overeenkomen. Controleer de preview voordat je bevestigt.

Een gekoppeld concept blijft alleen in het werkgeheugen en ondersteunt de bestaande **Terug / Opnieuw / Herstel**-acties. Een document wordt alleen naar de door de gebruiker gekozen provider geschreven na een **aparte** actie **Bewaar kopie SRT/VTT…**. De oorspronkelijke bestanden worden niet aangepast. Een externe Android-documentprovider kan daarna buiten Tuddy om synchroniseren.

## Grenzen

- Beide bestanden moeten geldige, expliciet gekozen UTF-8 SRT of WebVTT zijn (per bestand maximaal 256 KiB en 500 regels).
- Elke uiteindelijke SRT- en WebVTT-kopie blijft beperkt tot 500 regels en 256 KiB; individuele tekstregels, tijdcodes en controletekens volgen de bestaande strikte editorvalidatie.
- Als twee sporen niet veilig gecombineerd kunnen worden, wordt bevestiging uitgeschakeld. Probeer dan de afzonderlijke tijdcodemodus of kortere bestanden.
- Er worden geen spraak-/vertaalmodellen, netwerkoproepen, verborgen imports of automatisch opslaan gestart. De bestaande fysieke vliegtuigmodus-, licentie- en releasegates blijven ongewijzigd.
