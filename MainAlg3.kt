import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun gaussianKernel(size: Int, sigma: Double): Array<DoubleArray> {
    val kernel = Array(size) { DoubleArray(size) }
    val mean = size / 2
    var sum = 0.0

    for (y in 0 until size) {
        for (x in 0 until size) {
            val dy = y - mean
            val dx = x - mean
            val value = kotlin.math.exp(-(dx*dx + dy*dy) / (2 * sigma * sigma))
            kernel[y][x] = value
            sum += value
        }
    }

    for (y in 0 until size)
        for (x in 0 until size)
            kernel[y][x] /= sum

    return kernel
}

fun convolve2D(data: Array<IntArray>, kernel: Array<DoubleArray>): Array<DoubleArray> {
    val h = data.size
    val w = data[0].size
    val k = kernel.size
    val kHalf = k / 2

    val output = Array(h) { DoubleArray(w) }

    for (y in 0 until h) {
        for (x in 0 until w) {
            var sum = 0.0
            for (ky in 0 until k) {
                for (kx in 0 until k) {
                    val yy = (y + ky - kHalf).coerceIn(0, h - 1)
                    val xx = (x + kx - kHalf).coerceIn(0, w - 1)
                    sum += data[yy][xx] * kernel[ky][kx]
                }
            }
            output[y][x] = sum
        }
    }
    return output
}

fun cropMatrix(
    data: Array<IntArray>,edge: Int
): Array<IntArray> {

    val newHeight = data.size - 2*edge
    val newWidth = data[0].size - 2*edge

    return Array(newHeight) { y ->
        IntArray(newWidth) { x ->
            data[y + edge][x + edge]
        }
    }
}

fun saveImage(image: BufferedImage, fileName: String, format: String = "png") {
    val folder = File("hits/")
    if (!folder.exists()) folder.mkdirs()

    val file = File(folder, fileName)
    ImageIO.write(image, format, file)
}

fun maxPool2D(data: Array<IntArray>, poolSize: Int = 2, stride: Int = 2): Array<IntArray> {
    val height = data.size
    val width = data[0].size

    val outH = (height - poolSize) / stride + 1
    val outW = (width - poolSize) / stride + 1

    return Array(outH) { y ->
        IntArray(outW) { x ->
            var max = Int.MIN_VALUE
            for (dy in 0 until poolSize) {
                for (dx in 0 until poolSize) {
                    val v = data[y * stride + dy][x * stride + dx]
                    if (v > max) max = v
                }
            }
            max
        }
    }
}

fun main() {
    var data: MutableMap<String, BufferedImage> = mutableMapOf()
    var icc=1
    val basepath = "res/img"
    for(i in 1..4){
        val path= basepath+i.toString()+".png"
        val key = "img"+i+".png"
        val imgr: BufferedImage = ImageIO.read(File(path))
        data[key]=imgr
    }

    val edge = 30
    val kernelSize = 3
    var imc = 0
    val alpha = 0.05
    val theta = 180

    var fAvg: Array<DoubleArray>? = null

    for (entry in data) {
        val i = entry.value
        val name= entry.key
        val height = i.height
        val width = i.width

        val FSum = Array(height) { IntArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb = i.getRGB(x, y)
                val r = (rgb ushr 16) and 0xFF
                val g = (rgb ushr 8) and 0xFF
                val b = rgb and 0xFF
                FSum[y][x] = r + g + b
            }
        }

        val e = minOf(edge, (height - 1) / 2, (width - 1) / 2).coerceAtLeast(0)
        val FCrop = if (e > 0) cropMatrix(FSum, e) else FSum

        val fPool = maxPool2D(FCrop, 2)
        val gKernel = gaussianKernel(size = kernelSize, sigma = 1.0)
        val Fgaussian = convolve2D(fPool, gKernel)

        if (fAvg == null || fAvg!!.size != Fgaussian.size || fAvg!![0].size != Fgaussian[0].size) {
            fAvg = Array(Fgaussian.size) { DoubleArray(Fgaussian[0].size) { 0.0 } }
        }

        if(imc<=0){
            val h2 = Fgaussian.size
            val w2 = Fgaussian[0].size
            var hit = false
            val maxPool = fPool.maxOf { it.maxOrNull() ?: 0 }
            val maxG = Fgaussian.maxOf { it.maxOrNull() ?: 0.0 }
            val maxAvg = fAvg!!.maxOf { it.maxOrNull() ?: 0.0 }
            println("imc=$imc maxPool=$maxPool maxG=$maxG maxAvg=$maxAvg")
            var sum = 0.0
            var sum2 = 0.0
            var n = 0
            for (row in fPool) for (v in row) {
                val dv = v.toDouble()
                sum += dv
                sum2 += dv * dv
                n++
            }
            for (y in 0 until h2) {
                for (x in 0 until w2) {
                    if (fPool[y][x] > 4.0 * fAvg!![y][x] && fPool[y][x] > theta) {
                        saveImage(i, name)
                        icc++
                        hit = true
                        break
                    }
                }
                if (hit) break
            }
        } else {
            imc -= 1
        }

        val h2 = minOf(fAvg!!.size, Fgaussian.size)
        val w2 = minOf(fAvg!![0].size, Fgaussian[0].size)
        for (y in 0 until h2) {
            for (x in 0 until w2) {
                fAvg!![y][x] = (1 - alpha) * fAvg!![y][x] + alpha * Fgaussian[y][x]
            }
        }
    }
}
