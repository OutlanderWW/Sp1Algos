import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
/*
fun saveImage(image: BufferedImage, fileName: String, format: String = "png") {
    val folder = File("hits_Alg2/")
    if (!folder.exists()) folder.mkdirs()

    val file = File(folder, fileName)
    ImageIO.write(image, format, file)
}
*/
fun main() {
    var data: MutableMap<String, BufferedImage> = mutableMapOf()
    val basepath = "res/img"
    for (i in 1..216) {
        val path = basepath + i.toString() + ".png"
        val key = "img" + i + ".png"
        val imgr: BufferedImage = ImageIO.read(File(path))
        data[key] = imgr
    }
    val zSize = 5
    var threshold = 0.0
    var icc = 1

    var heatMap: Array<DoubleArray>? = null
    var pixScores: Array<DoubleArray>? = null

    for (entry in data) {
        val i = entry.value
        val name = entry.key
        val height = i.height
        val width = i.width

        if (heatMap == null || heatMap!!.size != height || heatMap!![0].size != width) {
            heatMap = Array(height) { DoubleArray(width) }
            pixScores = Array(height) { DoubleArray(width) }
            threshold = 0.0
        }

        val Total = Array(height) { DoubleArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb = i.getRGB(x, y)
                val r = (rgb ushr 16) and 0xFF
                val g = (rgb ushr 8) and 0xFF
                val b = rgb and 0xFF
                Total[y][x] = (r + g + b).toDouble()
            }
        }

        val diff = Array(height) { DoubleArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                diff[y][x] = Total[y][x] - heatMap!![y][x]
            }
        }

        val alpha = 0.03
        for (y in 0 until height) {
            for (x in 0 until width) {
                val h = maxOf(heatMap!![y][x], 5.0)
                val d = diff[y][x]
                pixScores!![y][x] = if (Total[y][x] > 25.0 && d > 3.0 * h) d * d else 0.0
            }
        }

        val tMapW = width / zSize
        val tMapH = height / zSize
        val blockScores = Array(tMapH) { DoubleArray(tMapW) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val by = y / zSize
                val bx = x / zSize
                if (by < tMapH && bx < tMapW) {
                    blockScores[by][bx] += pixScores!![y][x]
                }
            }
        }

        val flat = blockScores.flatMap { it.asList() }.sorted()
        val n = flat.size

        val p = 0.50
        val bg = flat[(p * (n - 1)).toInt()]
        val candidateThreshold = bg

        var hit = false

        outer@ for (by in 0 until tMapH) {
            for (bx in 0 until tMapW) {
                if (blockScores[by][bx] > 1.35 * candidateThreshold) {
                    hit = true
                    break@outer
                }
            }
        }

        if (hit) {
            saveImage(i, name)
            icc++
        } else {
            threshold = candidateThreshold
            for (y in 0 until height) {
                for (x in 0 until width) {
                    heatMap!![y][x] = alpha * Total[y][x] + (1 - alpha) * heatMap!![y][x]
                }
            }
        }
    }
}

