import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun saveImage(image: BufferedImage, fileName: String, format: String = "png") {
    val folder = File("hits/")
    if (!folder.exists()) folder.mkdirs()

    val file = File(folder, fileName)
    ImageIO.write(image, format, file)
}
fun main() {
    val img: BufferedImage = ImageIO.read(File("res/img4.jpg"))
    var data = listOf(img)
    val basepath = "res/img"
    for(i in 1..3){
        val path= basepath+i.toString()+".png"
        val imgr: BufferedImage = ImageIO.read(File(path))
        data=data+imgr
    }
    data=data.shuffled()
    var resolution = listOf(1024,1024)
    val zSize = 5
    var tMapW = resolution[1]/zSize
    var tMapH = resolution[0]/zSize
    val heatMap = Array(resolution[0]){DoubleArray(resolution[1])}
    val pixScores = Array(resolution[0]){DoubleArray(resolution[1])}
    var threshold = 0.toDouble()
    for (i in data){
        resolution= listOf(i.width,i.height)
        tMapH= resolution[0]/zSize
        tMapW= resolution[1]/zSize
        val Total = Array(resolution[0]){DoubleArray(resolution[1])}
        for(y in 0 until i.height){
            for(x in 0 until i.width){
                val rgb = i.getRGB(x, y)
                val r = (rgb ushr 16) and 0xFF
                val g = (rgb ushr 8) and 0xFF
                val b = rgb and 0xFF
                Total[x][y] = (r+g+b).toDouble()
            }
        }
        val diff = Array(resolution[0]){DoubleArray(resolution[1])}
        for(id in 0..resolution[0]-1){
            for(j in 0..resolution[1]-1){
                diff[id][j] = Total[id][j]-heatMap[id][j]
            }
        }
        for(a in 0..resolution[0]-1){
            for(b in 0..resolution[1]-1){
                if((Total[a][b]>4) and (diff[a][b]>4*heatMap[a][b])){
                    pixScores[a][b] = diff[a][b]*diff[a][b]
                }
                else{
                    pixScores[a][b] = 0.toDouble()
                }
                heatMap[a][b]+=0.03*Total[a][b]+ (1-0.03)*heatMap[a][b]
            }
        }
        val blockScores = Array(tMapW){DoubleArray(tMapH)}
        for(a in 0..tMapH){
            for(b in 0..tMapW){
                blockScores[a/zSize][b/zSize]+=pixScores[a/zSize][b/zSize]
            }
        }
        var max=Double.MIN_VALUE
        var second=Double.MIN_VALUE
        for(ib in blockScores){
            for(j in ib){
                when{
                    j>max ->{
                        second=max
                        max= j
                    }
                    j>second && j!=max->{
                        second=j

                    }
                }
            }
        }
        if(threshold==0.toDouble()){
            threshold=second
        }
        else{
            threshold=0.1*second+0.9*threshold
        }
        var breaker: Boolean=false
        var icc =1
        for(a in 0..tMapW-1){
            for(b in 0..tMapH-1){
                if(blockScores[a][b]>2*threshold){
                    saveImage(i, "hit"+icc+".png")
                    breaker=true
                    break
                    icc++
                }

            }
            if(breaker){
                break
            }
        }
    }

}