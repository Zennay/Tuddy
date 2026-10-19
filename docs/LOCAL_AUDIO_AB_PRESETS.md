# Tuddy · eigen lokale A–B oefenfragmenten (zonder opslag)

In de bestaande Android-audiospeler kan de gebruiker maximaal acht vrije audiomarkeringen plaatsen en daar twee als A/B aanwijzen. Nieuw is een tijdelijke oefenlijst van maximaal vier verschillende geldige A–B-combinaties (0,75 s tot 5 minuten).

De gebruiker kan de huidige A/B-combinatie **bewaren als oefenfragment**, daarna andere markeringen als aparte fragmenten bewaren, fragmenten in de lijst omhoog/omlaag verplaatsen, een eerder fragment direct weer activeren of het verwijderen. Activeren springt naar de juiste audiopositie, beëindigt concurrerende ondertitel-/passagelussen, en start de bestaande vrije A/B-herhaalmodus; bij gepauzeerde audio kan de gebruiker bewust weer afspelen. De eerder gemergede herhaalkeuze Doorlopend / 1× / 3× / 5× blijft bruikbaar per geactiveerd fragment.

Alle presets leven uitsluitend zolang deze lokale audiovoorvertoning open is. Een verwijderde audiomarkering verwijdert automatisch afhankelijke opgeslagen fragmenten. Verwijdering van het op dat moment herhaalde presetfragment stopt die herhaalmodus. De app schrijft geen audiobestanden, ondertitelbestanden, labels, cloud-/modelcalls, accountgegevens of opnames naar opslag; Android SAF-eigen provider kan externe eigen brondata laden op initiatief van de gebruiker.

Het werk is een native Compose-productwijziging, nog geen bewijs van hardware-/vliegtuigmodusacceptatie, licenties of getekende release. Verplichte CI, bronreview, security en release gates blijven ongewijzigd.
