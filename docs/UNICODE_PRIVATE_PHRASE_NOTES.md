# Volledige Unicode-letters in eigen Kurmanji/Sorani/Pasjtoe-zinnen

Eigen notities, herhaalkaarten en zelfgeschreven betekenisvelden volgen nu
dezelfde Unicode-invoerregels. Geldige aanvullende codepunten uit een
volledig UTF-16-surrogaatpaar mogen in persoonlijke woorden en zinnen
voorkomen; losse halve paren worden altijd geweigerd. Unicode-
besturingstekens, bidi-override/format-tekens en regelseparators blijven
geblokkeerd, met behoud van de bestaande uitzondering voor interne ZWNJ
die in Arabische schriften wordt gebruikt.

De bestaande privézinnenlijst (`PrivatePhraseDeck`), de
gewone zinneneditor, de audio-ondertitel-oefenkaart en de nieuwe
ondertitel-naar-eigen-zin-functie gebruiken dezelfde veiligheidsgrens.
De editors overschrijven geen invoer met een `take(120)` of
`take(180)` als dat een Unicode-teken middenin zou knippen.
Bestaande omvangslimieten, expliciete opnamekeuze, opslag en
stille foutafhandeling blijven geldig.

Dit levert geen automatische Kurmanji/Sorani/Pasjtoe-vertaling of
spraakherkenning en verandert niets aan externe licentie-, toestel-,
native-speaker- of productieacceptatie.
