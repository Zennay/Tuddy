# Ontbrekende offline tekstvertalingen opnieuw proberen

In **Tweeweg-tekstgesprek** toont Tuddy hoeveel eerder ingevoerde
gespreksregels in de gekozen taalrichting de status
**Model ontbreekt** hebben. Wanneer de benodigde ML Kit-tekstmodellen
daadwerkelijk als **lokaal aanwezig** worden bevestigd, kan de gebruiker
op **Herprobeer ontbrekende vertalingen** tikken.

Dit is een bewust gekozen handeling:

- Maximaal twaalf regels per tik, met exact hun eigen opgeslagen
  bron- en doeltaalcodes; geen vertalingen uit andere richtingen
  worden omgezet of als voltooid gemarkeerd.
- De oude foutstatus verandert tijdelijk naar **Bezig**; alleen
  echt lokaal teruggekomen ML Kit-resultaten krijgen
  **Lokaal vertaald**.
- Elke nieuwe poging krijgt een hogere revisie, zodat een
  eerdere, vertraagde vertaling het nieuwe resultaat niet kan
  overschrijven.
- Nieuwe netwerkaanvragen of modeldownloads worden **niet**
  automatisch gestart. De bestaande afzonderlijke
  **Modellen via wifi**-actie blijft verplicht voor ontbrekende
  taalpakketten.
- Zinnen blijven in de bestaande begrensde, tijdelijke
  gespreksgeschiedenis. Geen automatische transcriptopslag,
  cloudspraak, extra permissies of achtergrondupdates.

Tuddy kiest geen woorden of vertalingen voor Kurmanji, Sorani
of Pasjtoe buiten de expliciet beperkte eigen zinnen-/woordenmodule.
Afhankelijk van toestel, gelicentieerde taalpakketten en native-speaker
controle blijven alle relevante acceptatiegates open.
