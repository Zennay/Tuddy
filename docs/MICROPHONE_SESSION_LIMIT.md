# Privé microfoon-stop: maximaal 5, 15 of 30 minuten

Via **Taal- en modelinstellingen → Privacy · maximale microfoonsessieduur**
kan de gebruiker kiezen voor **Geen** (standaard), **5 min**,
**15 min** of **30 min**.

Wanneer je **Luister** aantikt, begint de optionele tijdslimiet.
Als de limiet verloopt, stopt Tuddy de Android on-device
spraakherkenner volledig met dezelfde bestaande **Stop**-logica
en toont een melding. De RAM-only ondertitels en bewust
opgeslagen studiegegevens blijven intact; start desgewenst
zelf een nieuwe sessie.

De klok telt over de **hele** luistersessie: wanneer Android
voor een volgende zin de SpeechRecognizer automatisch
herstart, wordt de deadline **niet** verlengd. Handmatig
Stop of de app naar de achtergrond brengen trekt de geplande
stop meteen in. Een oude callback kan de microfoon niet
alsnog aanzetten omdat de bestaande sessiegeneraties worden
ingetrokken. Als de timer niet gepland kan worden, stopt
de microfoon uit voorzorg.

Een andere limiet kiezen terwijl Tuddy luistert laat de
nieuwe duur **vanaf die wijziging** lopen; Geen schakelt
de timer uit. Alleen de gekozen minutenwaarde staat in
Androids private instellingen. Er worden geen starttijden,
audio, tekst, sessieduurgeschiedenis of gebruiksstatistieken
opgeslagen of verstuurd.

De timer geldt voor **microfoonopvang**, niet voor
handmatig ingevoerde tekst, het bekijken van historische
ondertitels of het lezen van opgeslagen zinnen. Deze optie
geeft geen toestemming om audio op te slaan, gebruikt
nooit netwerkspraak en maakt geen achtergrondservice aan.

Het is een extra privacycontrole, geen bewijs van
gevalideerde offline taalnauwkeurigheid, fysieke
toestelvalidatie of getekende Android-release.
