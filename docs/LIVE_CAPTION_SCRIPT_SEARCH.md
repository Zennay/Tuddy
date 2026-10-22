# Scriptbewust zoeken in het tijdelijke gesprek

De knop **Zoek in gesprek** doorzoekt de oorspronkelijke tekst, de vertaling en
eventuele eigen betekenisnotities van tijdelijke gespreksregels. Het gebruikt
dezelfde beperkte, alleen-voor-zoeken normalisatie als het persoonlijke
offline zinnenboek.

Bij het zoeken mogen de Arabische yeh (`ي`), alef maksura (`ى`) en
Perzische yeh (`ی`) onderling overeenkomen. Hetzelfde geldt voor Arabische
kaf (`ك`) en Perzische keheh (`ک`). De zoekfunctie negeert optionele
Arabische klinkertekens, tatweel en ZWNJ en normaliseert Unicode-
presentatievormen.

De normalisatie geldt uitsluitend voor de zoekopdracht en vergelijking in
RAM. De oorspronkelijke captions, modelvertalingen en persoonlijke
betekenissen blijven exact ongewijzigd; er wordt geen zoekindex opgeslagen.
Vorige/volgende navigatie gebruikt nog steeds de gevonden regels uit het
huidige tijdelijke gesprek. Als het gesprek wordt gewist, verdwijnt ook
deze tijdelijke zoekcontext.

Deze tolerantie dekt geen volledige dialect- of spellingconversie:
bijvoorbeeld `ه` en `ە`, of verschillende hamza-vormen, blijven
onderscheiden. Zoeken maakt geen vertaling en bevestigt geen taalcorrectheid.
