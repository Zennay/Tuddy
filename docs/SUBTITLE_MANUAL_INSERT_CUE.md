# Een nieuwe ondertitelregel toevoegen aan SRT/WebVTT

Open vanuit **Gesprekstools → Open SRT/WebVTT…** bewust een
ondertitelbestand. De lokale privédraft-editor biedt nu
**+ Nieuwe ondertitel…**, naast de bestaande zoek-/vervang-,
tijd-, split-/merge- en herstelopties.

Vul de **begin- en eindtijd** zelf in, bijvoorbeeld
`00:02:03.000` en `00:02:06.500`, en schrijf de
ondertiteltekst. Tuddy plaatst de regel op **chronologische
begintijd** in de bestaande cue-lijst. Bestaande regels,
hun inhoud en tijdcodes blijven ongemoeid. Bij gelijke
begintijd wordt de nieuwe regel achter de bestaande regels
met die begintijd geplaatst.

De app weigert negatieve tijden, een einde vóór het begin,
lege tekst, bestandsgrenzen > 256 KiB, te lange tekst of
een document met al 500 cues. Je kiest de tijden handmatig:
er wordt **geen audio beluisterd of gesynchroniseerd**,
geen tekst automatisch vertaald, en er is geen cloud- of
modelactiviteit.

De knop voegt uitsluitend toe aan het **vluchtige concept**
in het appgeheugen. Met **Herstel** verwijder je alle
conceptbewerkingen. Alleen **Bewaar kopie SRT…/VTT…** met
een afzonderlijke bevestiging en zelfgekozen Android-
bestandslocatie kan een nieuw document schrijven. Het
eerder geopende origineel blijft behouden.
