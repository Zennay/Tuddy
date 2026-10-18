# Live woordfocus ook binnen het normale gesprekscherm

Tuddy gebruikt nu in zowel de fullscreen leesmodus als het gewone
live transcript **dezelfde** Compose-weergave voor het laatste
herkende woord. Terwijl Androids bestaande on-device herkenner
tussenresultaten doorgeeft, verandert de visuele nadruk mee. Het
gewone gesprek behoudt compactere lettergrootte; de fullscreen
leesmodus houdt haar grote typografie.

De grens is expliciet zichtbaar: de markering is de laatste
witruimtegescheiden teksttoken, **niet** het op een exact
audiotijdstip daadwerkelijk uitgesproken woord. We berekenen geen
woordtijden, stemmen, speaker-identiteiten of uitlijning van de
vertaling. De voorlopig lokaal vertaalde tekst blijft apart en
krijgt bewust geen fictieve per-woordfocus.

De bestaande privacy- en lifecycle-regels blijven: geen nieuwe
audio-opname of server, geen extra rechten of vertaalmodeldownloads
en wissen bij stoppen/achtergrond zoals in de live recorder.
Unicode-witruimte wordt gebruikt, niet een Engels-only tokenizer.
Dit is broncode-integratie, geen bewijs van een getekende release
of fysiek vliegtuigmodusresultaat.
