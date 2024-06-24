import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.kandy.dsl.continuous
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.bars
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import org.jetbrains.kotlinx.kandy.letsplot.settings.LineType
import org.jetbrains.kotlinx.kandy.letsplot.x
import org.jetbrains.kotlinx.kandy.letsplot.y
import org.jetbrains.kotlinx.kandy.util.color.Color
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

const val url = "https://gtfo.fandom.com"
const val statSubUrl = "/wiki/Weapon_Stats"
const val infoBoxTableName = "infoboxtable"

fun main()
{
    val weaponStatPage: Document = Jsoup.connect(url + statSubUrl).get()
    val weaponStatTables = weaponStatPage.getElementsByClass("wikitable")
    val collectedWeapons: List<List<String>> =
        weaponStatTables.map { table ->
            table.allElements
                .select("tr > td > a")
                .select("[title]")
                .map { it.getElementsByAttributeValueStarting("href", "/wiki").attr("href") }
                .filter { it.isNotBlank() }
        }

    // Category > WeaponName, Details > Key, Value
    val collectedWeaponStats: MutableList<MutableMap<String, MutableMap<String, String>>> = mutableListOf()

    collectedWeapons.forEachIndexed { index, weapons ->
        collectedWeaponStats.add(index, mutableMapOf())

        weapons.forEach { weapon ->
            val infoPage = Jsoup.connect(url + weapon).get().getElementsByClass(infoBoxTableName).first() ?: return@forEach
            val name = infoPage.getElementsByClass("infoboxname").first()?.text() ?: return@forEach

            collectedWeaponStats[index][name] = mutableMapOf()

            val elements = infoPage.allElements.select("tr")
            elements.forEach elementLoop@{ element ->
                val category = element.select("td").first()?.select("div")?.first()?.text() ?: return@elementLoop
                val value = element.select("td").getOrNull(1)?.text() ?: return@elementLoop

                collectedWeaponStats[index][name]?.set(category, value) ?: return@elementLoop
            }
        }
    }

    val weaponData = dataFrameOf(
        "weapon" to collectedWeaponStats[0].keys.toList(),
        "damage" to collectedWeaponStats[0].values.map {
            val rawDamageData = it["Damage"] ?: return@map 0

            if (rawDamageData.contains("="))
                return@map rawDamageData.split(" ").first().substring(1).toFloatOrNull() ?: 0f
            else
                return@map rawDamageData.toFloatOrNull() ?: 0f
        },
    )

    val plot = weaponData.plot {
        x("weapon")
        y("damage")

        bars { }

        layout.title = "Weapon Damage per Refill"
    }

    plot.save("weather_plot.png") // Save the plot as an image
}