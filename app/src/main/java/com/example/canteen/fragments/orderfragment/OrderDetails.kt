package com.example.canteen.fragments.orderfragment

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.canteen.R
import com.example.canteen.models.Cart
import com.example.canteen.models.Order
import com.example.canteen.models.OrderDetails
import com.example.canteen.utilities.Constants
import com.example.canteen.utilities.showToast
import com.example.canteen.viewmodels.CartViewModel
import com.example.canteen.viewmodels.OrderViewModel
import com.example.composetutorialsample.ui.theme.LightBlue

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun OrderDetail(orderViewModel: OrderViewModel,orderList: List<Order>) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .background(colorResource(id = R.color.background))
    ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items(items = orderList, key = { it.id!!}) { order ->
                    //滑动删除
                    val dismissState = rememberDismissState()
                    if (dismissState.isDismissed(DismissDirection.EndToStart)) {
                        orderViewModel.deleteOrder(order) // 删除Order
                    }
                    ItemCart(
                        modifier = Modifier
                            .animateItemPlacement(),
                        dismissState = dismissState,
                        order = order
                    )
                }
            }

    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ItemCart(
    order: Order,
    dismissState:DismissState,
    modifier: Modifier = Modifier
) {

    SwipeToDismiss(
        state = dismissState,
        modifier = modifier,
        directions = setOf(DismissDirection.EndToStart),
        dismissThresholds = { direction ->
            FractionalThreshold(0.5f)
        },
        background = {
            dismissState.dismissDirection ?: return@SwipeToDismiss
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    DismissValue.Default -> Color.LightGray
                    DismissValue.DismissedToEnd -> Color.Green
                    DismissValue.DismissedToStart -> Color.Red
                }
            )
            val alignment = Alignment.CenterEnd
            val icon = Icons.Default.Delete
            val scale by animateFloatAsState(
                if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f
            )

            Box(
                Modifier
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .fillMaxSize()
                    .background(color, shape = RoundedCornerShape(6.dp)),
                contentAlignment = alignment
            ) {
                Icon(
                    icon,
                    contentDescription = "Localized description",
                    modifier = Modifier
                        .scale(scale)
                        .size(28.dp)
                )
            }
        },
        dismissContent = {
            Card(
                backgroundColor = Color.White,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                elevation = animateDpAsState(
                    if (dismissState.dismissDirection != null) 4.dp else 1.dp
                ).value
            ) {
                ItemCardContent(order)
            }
        }
    )
}


@Composable
fun ItemCardContent(order: Order) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(start = 8.dp, top = 12.dp, bottom = 12.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {

        Row {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, top = 8.dp)
            ) {
                var totalPrice = 0f
                order.detailsList?.forEach {
                    totalPrice += it.num * it.price
                }
                Text(text = "订单编号：${order.id} 日期：${order.cdate} 总价：$totalPrice")
                Text(text = "地址：${order.address} 电话：${order.phone}")
            }

            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) {
                        stringResource(R.string.show_less)
                    } else {
                        stringResource(R.string.show_more)
                    }
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .padding(start = 12.dp, top = 12.dp, bottom = 4.dp, end = 4.dp)

            ) {
                order.detailsList?.forEach { details ->
                    ItemCardContentDetails(details)
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemCardContentDetails(orderDetails: OrderDetails) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.color_item), shape = RoundedCornerShape(8.dp))
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)

    ) {
        GoodsImage(url = orderDetails.goods.thumbnail)
        Column(
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp)
        ) {
            Text(text = orderDetails.goods.name, maxLines = 1)
            GoodsPrice(orderDetails = orderDetails)
        }

    }


}

@Composable
fun GoodsPrice(orderDetails: OrderDetails) {
    val totalPrice = orderDetails.price * orderDetails.num
    Text(
        text = "单价:${orderDetails.price}\n金额:${totalPrice}",
        style = MaterialTheme.typography.body1,
        modifier = Modifier.padding(top = 6.dp)
    )

}

@Composable
fun GoodsImage(url: String) {
    val imageURL = Constants.NETWORK_DOMAIN + url
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageURL)
            .crossfade(true)
            .build(),
        contentDescription = stringResource(R.string.cart),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(128.dp)
            .clip(MaterialTheme.shapes.medium)
    )
}
