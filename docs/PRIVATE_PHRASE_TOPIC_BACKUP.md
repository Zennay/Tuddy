# Privéthema-indeling apart bewaren en herstellen

De bestaande eigen-zinnen-back-up bevat **geen** thema's. Daardoor
zou de indeling verloren gaan bij een nieuwe installatie. Nu kun je
los van de zinnen een tweede, expliciet gekozen JSON-bestand bewaren.

## Eerst zinnen, daarna thema's

1. Open **Mijn eigen offline zinnen → Mijn zinnenthema's beheren**.
2. Tik **Bewaar thema-indeling als aparte JSON…** en kies bewust
   zelf een locatie in de Android-bestandskiezer.
3. Op het nieuwe toestel importeer je **eerst je eigen zinnen**
   met de bestaande zinnen-JSON of TSV.
4. Open daarna weer de thema-manager en kies **Herstel
   thema-indeling uit JSON…**.
5. Tuddy controleert lokaal de versie, grootte, alle themanamen,
   unieke hash-identiteiten en de verwijzingen naar de daadwerkelijk
   opgeslagen zinnen. Onbekende of onvolledige bestanden worden geweigerd.
6. Pas na een tweede bevestiging worden maximaal twaalf thema's en
   tachtig toewijzingen als één app-private wijziging teruggezet.
   De oorspronkelijke eigen zinnen, betekenissen, favorieten en
   oefenplanning blijven onaangetast.

## Wat staat er in het bestand?

- Alleen een strikt JSON-document met versie, maximaal twaalf
  zelfbenoemde themanamen en een koppeling van maximaal tachtig
  **SHA-256-vingerafdrukken** aan die namen.
- Geen dubbele kopie van de zin, betekenis, microfoongegevens,
  AI-vertalingen, transcript, account of serveridentiteit.
- **Een SHA-256-hash is geen encryptie of gegarandeerde anonimisering:**
  een buitenstaander kan bij voorspelbare zinnen mogelijke teksten
  raden en vergelijken. Ook de eigen themanamen kunnen informatie
  onthullen. Kies daarom zorgvuldig waar je dit bestand opslaat.
- Android-bestandsproviders kunnen bestanden online synchroniseren.
  Bestanden buiten Tuddy worden niet automatisch gewist door
  **Wis mijn oefengegevens**. Er vindt geen stille export plaats.
- De vooraf gegenereerde snapshot wordt na terugkeer uit de
  Android-bestandskiezer opnieuw samengesteld. Een veranderde
  indeling wordt niet stilzwijgend met oude inhoud weggeschreven.
- Het herstel accepteert alleen hashes van zinnen die nu in de
  eigen zinnenlijst staan. Zo kunnen oude of vreemde themaverwijzingen
  niet terugkomen.
- De app laat bestaande offline spraakmodel-, licentie-, fysieke
  toestel- en productie-acceptatiegates intact.

De gewone zinnenback-up en aparte themaback-up zijn twee bewuste
bestanden. Een volledig eigen woordenboek of automatisch cloudaccount
wordt hiermee niet toegevoegd.
