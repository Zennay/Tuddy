# Offline luisteren vanuit de schermvullende leesmodus

De **Tuddy · Leesmodus** biedt nu een aparte, volledige breedteknop **🎙 Start offline luisteren** of **■ Stop microfoon**. Je hoeft de grote tweetalige ondertiteling niet meer te sluiten om de volgende gesprekszin op te nemen.

De startactie hergebruikt exact de bestaande Android-microfoonroute: een expliciete gebruiker-tik, Android `RECORD_AUDIO`-toestemming, lifecycle-veiligheid en een geïnstalleerde on-device spraakherkenningsmogelijkheid. Als het offline spraakpakket nog niet beschikbaar is of modelbeheer bezig is, blijft Start uitgeschakeld en verschijnt de uitleg dat je eerst het toestel gereed moet maken. Er is **geen cloudspraakfallback**, geen verborgen luisterstart en geen verlenging van de bestaande microfoon-tijdslimiet of verandering aan het stoppen op de achtergrond.

De status in het fullscreen-scherm blijft aangeven of de microfoon actief is. Zoeken, handmatige sprekerfilters, scrollen en tijdelijke bladwijzers veranderen de bestaande toestemmingen niet. Tijdens een actieve zoekopdracht blijft de microfoon los van het leesfilter doorgaan; voor teruggaan naar de nieuwste resultaten tik je op **Volg live**.

De aparte **privé-JSON-archieflezing** blijft uitsluitend lezen: er is daar geen startknop en geen microfoonmogelijkheid. Dit is geen acceptatiebewijs voor echte vliegtuigmodus of vrij gelicentieerde Kurmanji/Sorani/Pasjtoe-spraak- en vertaalmodellen. Die modellen, native-speakercontrole, release-signing en productie-admission blijven aparte gates.
