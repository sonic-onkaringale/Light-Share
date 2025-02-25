



fun debug(msg:String,tag:String="Debug",throwable: Throwable?=null)
{
    println(msg)
}

fun err(msg:String,tag:String="Debug",throwable: Throwable?=null)
{
    println(msg)
    throwable?.printStackTrace()
}
