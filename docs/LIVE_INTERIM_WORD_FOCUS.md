# Live onderschriften: voorlopig laatste woord benadrukken

Wanneer Androids strikt **on-device** spraakherkenner een voorlopig
transcript teruggeeft, toont Tuddy die originele zin in de schermvullende
leesmodus. Het laatste herkende niet-witruimtetoken is zichtbaar vet en in
de themakleur. Bij elk volgend resultaat verschuift die nadruk mee.

- Werkt ook met RTL-talen zoals Arabisch, Farsi en Urdu: woorden worden
  op Unicode-witruimte gescheiden, niet op het Engelse alfabet.
- De vertaling blijft afzonderlijk en wordt pas als afgerond beschouwd
  wanneer de bestaande lokale ML Kit-vertaling werkelijk slaagt.
- Het voorlopige scherm heeft de tekstmarkering **niet gesynchroniseerd op
  woordniveau met audio**. Er worden geen woordtijdstempels berekend,
  geïmproviseerd of opgeslagen; het is een visueel leesanker.
- De weergave bevat geen extra opname, remote audio-API, speaker-ID,
  tracking, persoonsgegevensopslag of nieuwe Android-permissie.
- Bij Stop, achtergrond of Wis gesprek verdwijnen de voorlopige woorden
  volgens de bestaande sessie-/microfoonregels.

De aanwezige tekst-, transcript- en spraakmodelbeperkingen blijven gelden;
voor Kurmanji, Sorani en Pasjtoe zijn nog geen getoetste vrije offline
spraak- en vertaalmodellen beschikbaar. Geen medische/juridische nauwkeurigheid
claimen zonder menselijke controle.
