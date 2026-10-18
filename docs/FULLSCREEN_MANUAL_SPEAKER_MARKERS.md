# Handmatige sprekers indelen vanuit Leesmodus

In de schermvullende live-sessieleesmodus kan elke afgeronde gespreksregel nu een **bewust handmatig** label krijgen. Tik op de knop onder de regel om door **onbekend → A → B → onbekend** te lopen. Het label verschijnt meteen boven de zin. De bestaande filter **Alle sprekers / Spreker A / Spreker B / Onbekend** reageert direct op diezelfde wijzigingen, ook terwijl de microfoon doorgaat.

Dit gebruikt exact de al bestaande `manuallyAssignedSpeakers` van de gewone gesprekslijst. Labels leven alleen in RAM, worden bij wissen of het verwijderen van de betreffende regel weggehaald en zijn geen biometrische herkenning, automatische diarization, vertaalbevestiging of registratie van personen. Er komt geen machine learning, extra microfoonverwerking, netwerkverbinding of persistente lijst bij.

De uit een bewust gekozen privé-JSON-archief geopende leesmodus blijft alleen-lezen: daar zijn de labelknoppen niet beschikbaar. Gekoppelde sprekers met dezelfde labelletter zijn niet noodzakelijk dezelfde mens. Beschikbaarheid van Kurdish/Pashto spraak/MT en de fysieke offline-/releasegates worden niet door labels aangetoond.
