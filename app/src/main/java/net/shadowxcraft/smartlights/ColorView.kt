import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View;
import net.shadowxcraft.smartlights.Color

class ColorView(context: Context) : View(context) {

    private var currentColor = Color()

    private var rectangle: Rect = Rect(0,0,this.width,this.height)
    private val paint: Paint = Paint()

    override fun onDraw(canvas: Canvas) {
        paint.setARGB(255, currentColor.red, currentColor.green, currentColor.blue)
        canvas.drawRect(rectangle, paint)
    }

    fun setColor(color: Color) {
        currentColor = color
        invalidate();
    }
}