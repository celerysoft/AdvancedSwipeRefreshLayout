# AdvancedSwipeRefreshLayout

## Features

* Pull to refresh

* Push to load more

* Custom header view

* Custom footer view

* Work as same as origin SwipeRefreshLayout, easy to get start

## Technical Information

* Required minimum API level: 15(Android 4.0.4)

* Supports all the screens sizes and density.

## Usage

### Step 1

#### Gradle

```
dependencies {
        compile 'com.celerysoft:advancedswiperefreshlayout:1.0.0'
}
```

### Step 2

Add the AdvancedSwipeRefreshLayout to your layout

```
<com.celerysoft.advancedswiperefreshlayout.AdvancedSwipeRefreshLayout  
        android:id="@+id/swipe_refresh_layout"  
        android:layout_width="match_parent"  
        android:layout_height="match_parent" />
```

### Step 3

`setOnPullToRefreshListener` or `setOnPushToLoadMoreListener` for the AdvancedSwipeRefreshLayout, you can see it in the demo.

## Screenshots

![Screenshot 1](https://raw.githubusercontent.com/celerysoft/README/master/AdvancedSwipeRefreshLayout/sc01.gif "Screenshot 1")

![Screenshot 2](https://raw.githubusercontent.com/celerysoft/README/master/AdvancedSwipeRefreshLayout/sc02.gif "Screenshot 2")

## Acknowledgement

* SwipeRefreshLayout

## License

[MIT](./LICENSE)
