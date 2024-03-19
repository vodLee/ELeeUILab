
# ELee UI Lab

阿益的UI实验室

这里存放整理我工作和生活中做的比较有意思的UI

### 可晃动分层布局 ShakeableLayerLayout

有没有看到过首页banner可以根据手机分层摇晃的？

好玩不，用这个就可以！

动画计算还不流畅，有机会再往下优化，但是现在满足正常使用了

使用方法极度简单，在子view里添加maxOffset，给个最大偏移量，一切都OK了

支持多个子view按照自己的想法摇晃

```xml
    <com.elee.eleeuilab.widget.ShakeableLayerLayout>
    
        <View
            android:layout_width="300dp"
            android:layout_height="300dp"
            android:layout_gravity="center"
            app:maxOffset="30dp" />
    </com.elee.eleeuilab.widget.ShakeableLayerLayout>
```