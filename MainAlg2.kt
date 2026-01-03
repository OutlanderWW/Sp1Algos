import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/*fun saveImage(image: BufferedImage, fileName: String, format: String = "png") {
    val folder = File("hits/")
    if (!folder.exists()) folder.mkdirs()

    val file = File(folder, fileName)
    ImageIO.write(image, format, file)
}*/
fun main() {
    val img: BufferedImage = ImageIO.read(File("res/img4.jpg"))
    var data = listOf(img)
    val basepath = "res/img"
    for(i in 1..3){
        val path= basepath+i.toString()+".png"
        val imgr: BufferedImage = ImageIO.read(File(path))
        data=data+imgr
    }
    val zSize = 5
    var threshold = 0.0
    var icc = 1

    var heatMap: Array<DoubleArray>? = null
    var pixScores: Array<DoubleArray>? = null

    for (i in data) {
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
                if ((Total[y][x] > 4) && (diff[y][x] > 4 * heatMap!![y][x])) {
                    pixScores!![y][x] = diff[y][x] * diff[y][x]
                } else {
                    pixScores!![y][x] = 0.0
                }
                heatMap!![y][x] = alpha * Total[y][x] + (1 - alpha) * heatMap!![y][x]
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

        var max = 0.0
        var second = 0.0
        for (row in blockScores) {
            for (v in row) {
                when {
                    v > max -> {
                        second = max
                        max = v
                    }
                    v > second && v != max -> {
                        second = v
                    }
                }
            }
        }

        threshold = if (threshold == 0.0) second else 0.1 * second + 0.9 * threshold
        // test można zakomentować
        val maxBlock = blockScores.maxOf { it.maxOrNull() ?: 0.0 }
        println("maxBlock=$maxBlock second=$second threshold=$threshold")

        var breaker = false
        for (by in 0 until tMapH) {
            for (bx in 0 until tMapW) {
                if (blockScores[by][bx] > 2 * threshold) {
                    saveImage(i, "hit$icc.png")
                    icc++
                    breaker=true
                    break
                }
            }
            if(breaker){
                break
            }
        }
    }

}