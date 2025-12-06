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

    // normalize kernel
    for (y in 0 until size)
        for (x in 0 until size)
            kernel[y][x] /= sum

    return kernel
}
fun convolve2D(
    data: Array<IntArray>,
    kernel: Array<DoubleArray>
): Array<DoubleArray> {

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
fun maxPool2D(
    data: Array<IntArray>,
    poolSize: Int = 2,
    stride: Int = 2
): Array<IntArray> {

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
    val img: BufferedImage = ImageIO.read(File("res/img4.jpg"))
    var data = listOf(img)
    var icc=1
    val basepath = "res/img"
    for(i in 1..3){
        val path= basepath+(i).toString()+".png"
        val imgr: BufferedImage = ImageIO.read(File(path))
        data=data+imgr
    }
    data=data.shuffled()
    var edge=30
    val kernel=3
    var imc=12
    val alpha = 0.2
    val theta = 255
    var resolution= listOf(data[0].width, data[0].height)
    var fAvg=Array(resolution[0]){DoubleArray(resolution[1])}
    for(i in data){
        resolution= listOf(i.width, i.height)
        var FSum= Array(resolution[0]){IntArray(resolution[1])}
        for(y in 0 until i.height){
            for(x in 0 until i.width){
                val rgb = i.getRGB(x, y)
                val r = (rgb ushr 16) and 0xFF
                val g = (rgb ushr 8) and 0xFF
                val b = rgb and 0xFF
                FSum[x][y] = r+g+b
            }
        }
        edge=(i.height+i.width)/30
        val FCrop=cropMatrix(FSum, edge)
        val newres = listOf(i.width,i.height)
        val fPool = maxPool2D(FCrop, 2)
        val kernel = gaussianKernel(size=5, sigma=1.0)
        val Fgaussian =convolve2D(fPool, kernel)
        if(imc<=0){
            for(a in 0..newres[1]){
                for(b in 0..newres[0]){
                    if((fAvg[a][b]*4.0<Fgaussian[a][b]) and (fPool[a][b] > theta)){
                        saveImage(i, "hit"+icc)
                    }
                }

            }
        }
        else imc-=1
        val size=fAvg.size
        if(!fAvg.any {row->row.any{it!=0.0}}){
            fAvg=Fgaussian
        }

        else{
            for(ia in 0..minOf(fAvg.size,Fgaussian.size)-1){
                for(j in 0..minOf(fAvg[0].size, Fgaussian[0].size)-1){
                    val favgc=(alpha-1)*fAvg[ia][j]+alpha*Fgaussian[ia][j]
                    fAvg[ia][j]=(alpha-1)*fAvg[ia][j]+alpha*Fgaussian[ia][j]
                }
            }




    }
}
}