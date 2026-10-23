# Focus op het laatste woord tijdens live lezen

De leesmodus houdt de visuele woordfocus vast wanneer Android een voorlopige
herkenningsregel omzet in een afgeronde sessieregel:

- Zolang voorlopige herkende tekst zichtbaar is, wordt het laatste tekstwoord
  van die voorlopige regel benadrukt.
- Zodra er geen voorlopige bronregel meer zichtbaar is, wordt het laatste
  tekstwoord van de meest recente afgeronde regel benadrukt, zowel in de bron
  als in de lokaal voltooide vertaling.
- Deze focus is alleen actief tijdens luisteren, wanneer **Volg live** aanstaat
  en de gebruiker niet zoekt, filtert, alleen bladwijzers bekijkt of een archief
  leest. Handmatig terugscrollen blijft vrij; de focus en automatische scroll
  worden niet teruggedwongen totdat de gebruiker **Volg live** kiest.
- Woorden blijven aanklikbaar voor de bestaande lokale woordkaart. Zoekmarkering
  blijft beschikbaar; de opgeslagen transcripttekst wordt niet aangepast.

## Nauwkeurigheidsgrens

Dit is een visuele focus op tekst die de on-device herkenner al heeft geleverd,
geen audiotijdstempel, geen diarization en geen woorduitlijning tussen spraak en
vertaling. Er worden geen extra microfoon-, model-, netwerk- of opslagacties
gestart. Echte Spotify-achtige audio-synchronisatie vereist betrouwbare
woordtijdstempels uit een goedgekeurde spraakengine; deze functie claimt die
niet.
