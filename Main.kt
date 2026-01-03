//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File

/*fun saveImage(image: BufferedImage, fileName: String, format: String = "png") {
    val folder = File("hits/")
    if (!folder.exists()) folder.mkdirs()

    val file = File(folder, fileName)
    ImageIO.write(image, format, file)
}*/

fun main() {
    val img: BufferedImage = ImageIO.read(File("res/img4.jpg"))
    var data = listOf(img)
    var icc=1
    val basepath = "res/img"
    for(i in 1..3){
        val path= basepath+i.toString()+".png"
        val imgr: BufferedImage = ImageIO.read(File(path))
        data=data+imgr
    }
    // values can be changed but these are suggested within the article
    val beta = 120
    val gamma = 80
    val delta = 0.04F

    for (i in data){
        val resolution = listOf(i.height, i.width)
        val FSum= Array(resolution[0]){IntArray(resolution[1])}
        for(y in 0 until i.height){
            for(x in 0 until i.width){
                val rgb = i.getRGB(x, y)
                val r = (rgb ushr 16) and 0xFF
                val g = (rgb ushr 8) and 0xFF
                val b = rgb and 0xFF
                FSum[y][x] = r + g + b
            }
        }
        val maxFsum = FSum.maxOf { row -> row.maxOrNull() ?: Int.MIN_VALUE }
        var FZeroesCount:Float = 0.0F
        for(iZ in FSum){
            for(j in iZ){
                if(j<=3) FZeroesCount+=1.0F
            }
        }
        var FSumSum:Float= 0.0F
        for(iSS in FSum){
            for(j in iSS){
                FSumSum+=j
            }
        }
        val pc: Float=resolution[0].toFloat()*resolution[1].toFloat()
        val avgFSum: Float = (FSumSum /pc)
        val blackFSum:Float = (FZeroesCount/pc)
        val avgsumVar=avgFSum<gamma
        val maxVar=maxFsum>beta
        val blackVar =blackFSum>delta
        println("max=$maxFsum avg=$avgFSum black=$blackFSum")
        if(maxVar and avgsumVar){
            saveImage(i, "hit"+icc+".png")
            icc+=1
        }
    }
}