# Scherm wakker houden tijdens live meelezen

In **Taal- en modelinstellingen → Toegankelijkheid** staat de
nieuwe schakelaar **Scherm wakker tijdens luisteren**. De standaard
is **uit**. Als je zelf inschakelt, vraagt Tuddy Android om het
scherm aan te houden **uitsluitend terwijl de eigen on-device
microfoonluisterstand actief is**.

- Zodra je op **Stop** tikt, de ingestelde sessietimer afloopt,
  de app naar de achtergrond verdwijnt of het scherm verdwijnt,
  verdwijnt ook het Android-venstervlaggetje. Het display volgt
  daarna weer je normale toestelinstellingen.
- De keuze is alleen één app-private Boolean. Tuddy bewaart voor
  deze functie geen ondertitels, audio, gebruikersactiviteit of
  levenscycluslog.
- Er is geen extra toestelpermissie, achtergrondservice of
  `WAKE_LOCK` nodig. Scherm wakker is **geen** garantie dat
  spraakherkenning blijft werken; de gebruikelijke on-device
  spraak-/pakketchecks en microfoonrechten blijven verplicht.

Een langer verlicht scherm gebruikt mogelijk meer batterij en toont
privégesprekken langer aan omstanders. De bestaande optionele
**privéweergave** kan schermafbeeldingen beperken, maar beschermt
niet tegen meelezen of externe camera's. Zet dit dus alleen aan
wanneer je het bewust nodig hebt. Fysieke toestelacceptatie,
modellicenties en signed release blijven open.
