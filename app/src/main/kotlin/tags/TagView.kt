package tags

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import org.btcmap.databinding.WidgetTagBinding

class TagView : LinearLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val binding: WidgetTagBinding by lazy {
        WidgetTagBinding.inflate(LayoutInflater.from(context), this, true)
    }

    var onDeleteListener: OnDeleteListener? = null

    init {
        binding.delete.setOnClickListener { onDeleteListener?.onDelete(this) }
    }

    fun getValue(): Pair<String, String> {
        return Pair(binding.name.text.toString(), binding.value.text.toString())
    }

    fun setValue(name: String, value: String) {
        binding.name.setText(name)
        binding.value.setText(value)
    }

    fun interface OnDeleteListener {

        fun onDelete(sender: TagView)
    }
}