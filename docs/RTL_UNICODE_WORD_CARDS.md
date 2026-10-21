# Offline woordkaarten voor samengestelde en RTL-schriften

In een eigen tekst, lokaal ondertitelconcept of bestaande transcriptregel kun
je op een woord tikken om een lokale woordkaart te openen. Woordselectie volgt
Unicode-codepunten: accenttekens die als afzonderlijke combinerende tekens
zijn opgeslagen blijven bij de basisletter. Kurmanji, Sorani en Pasjtoe
ondervinden daardoor minder onbedoelde afbrekingen bij gewone diakritische
tekens, apostrofs, koppeltekens en interne Arabische schriftjoiners. Dezelfde
logica ondersteunt codepunten buiten de BMP zonder een half surrogaat als
woord te bewaren.

De getoonde woord- en betekenisvelden volgen de inhoudsrichting (RTL/LTR).
De woordkaart bevat **geen** nieuwe vertaalmachine: alleen het kleine
gecontroleerde NL/EN/DE/FR/ES woordenboek kan automatisch een betekenis
tonen. Als de huidige productweergave een eigen-woordenfunctie aanbiedt, kan
de gebruiker zelf een niet-geverifieerde betekenis invullen. Niet ondersteunde
talen krijgen geen verzonnen woordenboekbetekenis.

Lange tokens boven 40 UTF-16-code-eenheden worden niet automatisch
afgekapt en opgeslagen. Woordenboekopslag, lokale uitspraak en privacyregels
behouden hun bestaande grenzen: geen cloudwoordenboek, modeldownload,
autodetectie, gesprekslogging of microfoontoegang door de woordkaart.

Dit is expliciet **geen** bewijs van volledige offline Kurmanji/Sorani/Pasjtoe
spraakherkenning of vertaling. Zie issues #1 en #2 voor die releasegates.
