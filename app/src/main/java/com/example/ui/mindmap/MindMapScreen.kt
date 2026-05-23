package com.example.ui.mindmap

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.MindMapNode
import com.example.ui.viewmodel.MindMapViewModel

// Layout node representing the flat coordinate computation
data class RenderNode(
    val node: MindMapNode,
    val x: Float,
    val y: Float,
    val parentX: Float?,
    val parentY: Float?,
    val direction: Float // -1f for left branch, 1f for right branch
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapScreen(
    viewModel: MindMapViewModel,
    onNavigateBack: () -> Unit
) {
    val rootNode by viewModel.activeRootNode.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val panOffset by viewModel.panOffset.collectAsState()
    val selectedNode by viewModel.selectedNode.collectAsState()
    
    var showNodeEditDialog by remember { mutableStateOf<MindMapNode?>(null) }
    var showAddNodeDialog by remember { mutableStateOf<MindMapNode?>(null) }
    var showXmlExportDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (rootNode == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Compute the full spatial structural layout of the tree
    val renderedNodes = remember(rootNode) {
        layoutEntireTree(rootNode!!)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        rootNode?.text ?: "思维导图阅读器",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回主面板")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetZoomAndPan() }) {
                        Icon(Icons.Default.FilterCenterFocus, contentDescription = "对齐视口")
                    }
                    IconButton(onClick = { showXmlExportDialog = true }) {
                        Icon(Icons.Default.Code, contentDescription = "查看 Freeplane 源码 (.mm)")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background // Adapt to Natural Tones theme background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val centerX = widthPx / 2f
            val centerY = heightPx / 2f

            val density = LocalDensity.current
            val cardWidthPx = with(density) { 180.dp.toPx() }
            val cardHeightPx = with(density) { 62.dp.toPx() }

            // Gesture workspace base (Canvas + overlays)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            viewModel.updatePan(pan)
                            viewModel.adjustZoom(zoom)
                        }
                    }
            ) {
                // 1. Connection lines layer
                MindMapConnectionLines(
                    nodes = renderedNodes,
                    scale = zoomScale,
                    offset = panOffset,
                    centerX = centerX,
                    centerY = centerY,
                    cardWidthPx = cardWidthPx
                )

                // 2. Interactive node overlays
                renderedNodes.forEach { renderNode ->
                    // Calculate visual screen position
                    val finalX = (renderNode.x * zoomScale) + panOffset.x + centerX
                    val finalY = (renderNode.y * zoomScale) + panOffset.y + centerY

                    // Offset calculations placing center of card on the logical (X, Y) layout anchor
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (finalX - (cardWidthPx / 2f) * zoomScale).toInt(),
                                    y = (finalY - (cardHeightPx / 2f) * zoomScale).toInt()
                                )
                            }
                            .size(180.dp, 62.dp)
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            )
                    ) {
                        NodeCardItem(
                            renderNode = renderNode,
                            isSelected = selectedNode?.id == renderNode.node.id,
                            onClick = {
                                viewModel.selectNode(renderNode.node)
                            },
                            onFoldToggle = {
                                viewModel.toggleNodeFolded(renderNode.node.id)
                            }
                        )
                    }
                }
            }

            // 3. Zoom controls hovering widget (Bottom-Left)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.adjustZoom(1.15f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ZoomIn,
                        contentDescription = "放大",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    "${(zoomScale * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { viewModel.adjustZoom(0.85f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ZoomOut,
                        contentDescription = "缩小",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 4. Details / Actions Drawer Panel (Bottom overlay if node chosen)
            AnimatedVisibility(
                visible = selectedNode != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                selectedNode?.let { node ->
                    NodeActionsPanel(
                        node = node,
                        isRootNode = node.id == rootNode?.id,
                        onClose = { viewModel.selectNode(null) },
                        onEdit = { showNodeEditDialog = node },
                        onAddSubnode = { showAddNodeDialog = node },
                        onDelete = {
                            viewModel.deleteNode(node.id)
                            viewModel.selectNode(null)
                        }
                    )
                }
            }
        }
    }

    // Dialogs
    showNodeEditDialog?.let { node ->
        EditNodeDialog(
            node = node,
            onDismiss = { showNodeEditDialog = null },
            onSave = { txt, clr, note ->
                viewModel.updateNodeDetails(node.id, txt, clr, note)
                showNodeEditDialog = null
            }
        )
    }

    showAddNodeDialog?.let { node ->
        AddSubnodeDialog(
            parentNode = node,
            onDismiss = { showAddNodeDialog = null },
            onAdd = { txt, clr ->
                viewModel.addChildNode(node.id, txt, clr)
                showAddNodeDialog = null
            }
        )
    }

    if (showXmlExportDialog) {
        val xmlText = viewModel.getActiveMindMapXml()
        ExportXmlDialog(
            xmlText = xmlText,
            onDismiss = { showXmlExportDialog = false }
        )
    }
}

/**
 * Draws the high-performance connecting curves behind the nodes.
 */
@Composable
fun MindMapConnectionLines(
    nodes: List<RenderNode>,
    scale: Float,
    offset: Offset,
    centerX: Float,
    centerY: Float,
    cardWidthPx: Float
) {
    val isDarkTheme = isSystemInDarkTheme()
    val dotColor = if (isDarkTheme) Color(0xFF44474E) else Color(0xFFC4C7C5)
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw Natural Tones dotted panning and zooming canvas grid pattern
        val gridSpacing = 24.dp.toPx() * scale
        if (gridSpacing > 8f) {
            val originX = offset.x + centerX
            val originY = offset.y + centerY
            
            // Limit coordinate loop to the size constraints of the visible canvas block
            val startX = originX + (((-originX) / gridSpacing).toInt() - 1) * gridSpacing
            val startY = originY + (((-originY) / gridSpacing).toInt() - 1) * gridSpacing
            
            var currX = startX
            while (currX < size.width + gridSpacing) {
                var currY = startY
                while (currY < size.height + gridSpacing) {
                    drawCircle(
                        color = dotColor.copy(alpha = 0.8f),
                        radius = (1.2f * scale).coerceIn(1.0f, 2.5f).dp.toPx(),
                        center = Offset(currX, currY)
                    )
                    currY += gridSpacing
                }
                currX += gridSpacing
            }
        }

        nodes.forEach { node ->
            if (node.parentX != null && node.parentY != null) {
                // Source pivot on the parent's edge
                // Sided source alignment: if heading right, parent right edge; if heading left, parent left edge.
                val parentEdgeOffset = node.direction * (cardWidthPx / 2f)
                val startX = (node.parentX * scale) + offset.x + centerX + parentEdgeOffset * scale
                val startY = (node.parentY * scale) + offset.y + centerY

                // Target pivot on child edge
                val childEdgeOffset = -node.direction * (cardWidthPx / 2f)
                val endX = (node.x * scale) + offset.x + centerX + childEdgeOffset * scale
                val endY = (node.y * scale) + offset.y + centerY

                // Draw curve using cubic Bézier connections
                val curvePath = Path().apply {
                    moveTo(startX, startY)
                    val controlX1 = startX + node.direction * (100f * scale)
                    val controlY1 = startY
                    val controlX2 = endX - node.direction * (100f * scale)
                    val controlY2 = endY
                    cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
                }

                // Smooth organic branch colors based on depth levels
                val strokeColor = when (node.direction) {
                    -1f -> Color(0xFF14B8A6).copy(alpha = 0.65f) // Sage Green
                    else -> Color(0xFFF59E0B).copy(alpha = 0.65f) // Soft Amber
                }

                drawPath(
                    path = curvePath,
                    color = strokeColor,
                    style = Stroke(
                        width = (2.2f * scale).coerceAtLeast(1.5f).dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

/**
 * Visual elements of an individual Node Card Composable.
 */
@Composable
fun NodeCardItem(
    renderNode: RenderNode,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFoldToggle: () -> Unit
) {
    val node = renderNode.node
    val isRoot = renderNode.parentX == null && renderNode.parentY == null
    val isLevel1 = !isRoot && renderNode.parentX == 0f && renderNode.parentY == 0f
    
    val isDarkTheme = isSystemInDarkTheme()
    
    val cardBg: Color
    val textColor: Color
    val borderColor: Color
    val cardBorderWidth = if (isSelected) 2.2.dp else 1.2.dp
    val cardShape = if (isRoot) RoundedCornerShape(24.dp) else RoundedCornerShape(12.dp)

    if (isRoot) {
        cardBg = if (isDarkTheme) Color(0xFFA8C8F9) else Color(0xFF001D35)
        textColor = if (isDarkTheme) Color(0xFF001D35) else Color.White
        borderColor = if (isSelected) {
            if (isDarkTheme) Color.White else Color(0xFF14B8A6)
        } else {
            Color.Transparent
        }
    } else if (isLevel1) {
        if (node.color != null) {
            val customCol = safeParseColor(node.color)
            cardBg = customCol.copy(alpha = 0.15f)
            textColor = if (isDarkTheme) Color.White else customCol
            borderColor = if (isSelected) customCol else customCol.copy(alpha = 0.5f)
        } else if (renderNode.direction == -1f) {
            // Left Branch Sage Green
            cardBg = if (isDarkTheme) Color(0xFF0D2D29) else Color(0xFFE6F4F1)
            textColor = if (isDarkTheme) Color(0xFF14B8A6) else Color(0xFF0D9488)
            borderColor = if (isSelected) {
                if (isDarkTheme) Color(0xFF14B8A6) else Color(0xFF0D9488)
            } else {
                if (isDarkTheme) Color(0xFF14B8A6).copy(alpha = 0.4f) else Color(0xFFBFE1DC)
            }
        } else {
            // Right Branch Sky Blue
            cardBg = if (isDarkTheme) Color(0xFF0A2240) else Color(0xFFD3E3FD)
            textColor = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF041E49)
            borderColor = if (isSelected) {
                if (isDarkTheme) Color(0xFF2563EB) else Color(0xFF041E49)
            } else {
                if (isDarkTheme) Color(0xFF2563EB).copy(alpha = 0.4f) else Color(0xFFA8C8F9)
            }
        }
    } else {
        // Deeper Children, clean white/surface body notes
        cardBg = MaterialTheme.colorScheme.surface
        textColor = MaterialTheme.colorScheme.onSurface
        borderColor = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {
        // Core Card Surface
        Card(
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = cardBg
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 2.dp)
                .border(
                    width = cardBorderWidth,
                    color = borderColor,
                    shape = cardShape
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Freeplane icons row mapped to beautiful representation emojis
                if (node.icons.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .background(
                                if (isDarkTheme) Color(0xFF121417) else Color(0xFFE2E8F0),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        node.icons.take(2).forEach { icon ->
                            Text(
                                emojiOfIcon(icon),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Core Plain Text
                Text(
                    text = node.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Note tiny document symbol
                if (node.note != null) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = "带备注",
                        tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 2.dp)
                    )
                }
            }
        }

        // Expand/Collapse bullet, positioned with sided ergonomic offsets
        if (node.children.isNotEmpty()) {
            val isFolded = node.folded
            val alignment = if (renderNode.direction == 1f) Alignment.CenterEnd else Alignment.CenterStart
            val indicatorBg = if (isRoot) {
                if (isDarkTheme) Color(0xFF001D35) else Color(0xFFD3E3FD)
            } else {
                borderColor
            }
            val indicatorTextCol = if (isRoot) {
                if (isDarkTheme) Color.White else Color(0xFF041E49)
            } else {
                if (isDarkTheme) Color.Black else Color.White
            }

            Box(
                modifier = Modifier
                    .align(alignment)
                    .offset(x = if (renderNode.direction == 1f) (0).dp else (0).dp)
                    .size(20.dp)
                    .background(indicatorBg, CircleShape)
                    .clickable(onClick = onFoldToggle),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFolded) "+${node.children.size}" else "-",
                    color = indicatorTextCol,
                    fontSize = if (isFolded) 9.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Control Drawer panel appearing at base on active selection.
 */
@Composable
fun NodeActionsPanel(
    node: MindMapNode,
    isRootNode: Boolean,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onAddSubnode: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Adjust, 
                        contentDescription = null, 
                        tint = node.color?.let { safeParseColor(it) } ?: MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "选定节点",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "收起", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Text text
            Text(
                node.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Inner Note Block if attached
            node.note?.let { noteText ->
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .padding(11.dp)
                ) {
                    Text(
                        "🗒️ 备注说明",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFD97706)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        noteText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Command buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Edit
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("编辑内容", fontSize = 12.sp)
                }

                // 2. Add
                Button(
                    onClick = onAddSubnode,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加子节点", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // 3. Delete (except Root)
                if (!isRootNode) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Edit dialog box
 */
@Composable
fun EditNodeDialog(
    node: MindMapNode,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var text by remember { mutableStateOf(node.text) }
    var note by remember { mutableStateOf(node.note ?: "") }
    var colorHex by remember { mutableStateOf(node.color ?: "") }

    val presetColors = listOf("", "#1A73E8", "#10B981", "#EA580C", "#9333EA", "#E11D48")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("📝 编辑思维节点", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("节点文本") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注说明 (可选)") },
                    shape = RoundedCornerShape(10.dp),
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Column {
                    Text("🎨 分支主题色彩", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetColors.forEach { colorStr ->
                            val colorVal = if (colorStr.isEmpty()) Color(0xFF64748B) else safeParseColor(colorStr)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(colorVal, CircleShape)
                                    .border(
                                        width = if (colorHex == colorStr) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { colorHex = colorStr },
                                contentAlignment = Alignment.Center
                            ) {
                                if (colorStr.isEmpty()) {
                                    Icon(Icons.Default.Block, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { if (text.isNotBlank()) onSave(text, colorHex, note) },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("保存修改", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Add sub-node dialog
 */
@Composable
fun AddSubnodeDialog(
    parentNode: MindMapNode,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf(parentNode.color ?: "") }

    val presetColors = listOf("", "#1A73E8", "#10B981", "#EA580C", "#9333EA", "#E11D48")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("➕ 添加下级子节点", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("将作为「${parentNode.text}」的子分支关联挂载。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("分支节点名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Column {
                    Text("💡 继承或设定专属颜色", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetColors.forEach { colorStr ->
                            val colorVal = if (colorStr.isEmpty()) Color(0xFF64748B) else safeParseColor(colorStr)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(colorVal, CircleShape)
                                    .border(
                                        width = if (colorHex == colorStr) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { colorHex = colorStr },
                                contentAlignment = Alignment.Center
                            ) {
                                if (colorStr.isEmpty()) {
                                    Icon(Icons.Default.Block, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { if (text.isNotBlank()) onAdd(text, colorHex) },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("建立关联", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Show the XML string
 */
@Composable
fun ExportXmlDialog(
    xmlText: String,
    onDismiss: () -> Unit
) {
    val localContext = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                Text("Freeplane .mm 源码面板", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("这是一份符合 Freeplane 联盟标准的文件内容, 任何桌面端 Freeplane 均可识别解析:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        xmlText,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("关闭", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(xmlText))
                            Toast.makeText(localContext, "📋 成功复制 Freeplane XML 码！", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("复制源码", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Recursive Position Calculations matching standard Freeplane layout nodes structure.
 */
fun layoutEntireTree(root: MindMapNode): List<RenderNode> {
    val outList = mutableListOf<RenderNode>()
    outList.add(RenderNode(root, 0f, 0f, null, null, 1f))

    if (root.children.isEmpty()) {
        return outList
    }

    // Allocate root's immediate descendants with opposite layouts to make a beautifully balanced layout.
    val leftChildren = mutableListOf<MindMapNode>()
    val rightChildren = mutableListOf<MindMapNode>()

    root.children.forEachIndexed { i, child ->
        when (child.side) {
            "left" -> leftChildren.add(child)
            "right" -> rightChildren.add(child)
            else -> {
                // If side undefined, split evenly
                if (i % 2 == 0) {
                    rightChildren.add(child.copy(side = "right"))
                } else {
                    leftChildren.add(child.copy(side = "left"))
                }
            }
        }
    }

    val X_GAP = 300f
    val Y_GAP = 34f

    // Layout right subtree
    if (rightChildren.isNotEmpty()) {
        val rightHeights = rightChildren.map { getSubtreeHeight(it) }
        val totalRightHeight = rightHeights.sum() + (rightChildren.size - 1) * Y_GAP
        var currentY = 0f - totalRightHeight / 2f

        rightChildren.forEachIndexed { index, child ->
            val height = rightHeights[index]
            val childY = currentY + height / 2f
            layoutSubtree(
                node = child,
                parentX = 0f,
                parentY = 0f,
                startX = X_GAP,
                startY = childY,
                direction = 1f,
                outList = outList
            )
            currentY += height + Y_GAP
        }
    }

    // Layout left subtree
    if (leftChildren.isNotEmpty()) {
        val leftHeights = leftChildren.map { getSubtreeHeight(it) }
        val totalLeftHeight = leftHeights.sum() + (leftChildren.size - 1) * Y_GAP
        var currentY = 0f - totalLeftHeight / 2f

        leftChildren.forEachIndexed { index, child ->
            val height = leftHeights[index]
            val childY = currentY + height / 2f
            layoutSubtree(
                node = child,
                parentX = 0f,
                parentY = 0f,
                startX = -X_GAP,
                startY = childY,
                direction = -1f,
                outList = outList
            )
            currentY += height + Y_GAP
        }
    }

    return outList
}

fun layoutSubtree(
    node: MindMapNode,
    parentX: Float,
    parentY: Float,
    startX: Float,
    startY: Float,
    direction: Float,
    outList: MutableList<RenderNode>
) {
    val nodeX = startX
    val nodeY = startY
    outList.add(RenderNode(node, nodeX, nodeY, parentX, parentY, direction))

    if (node.folded || node.children.isEmpty()) {
        return
    }

    val children = node.children
    val X_GAP = 280f
    val Y_GAP = 30f

    val childrenHeights = children.map { getSubtreeHeight(it) }
    val totalHeight = childrenHeights.sum() + (children.size - 1) * Y_GAP

    var currentY = nodeY - totalHeight / 2f
    for (i in children.indices) {
        val child = children[i]
        val height = childrenHeights[i]
        val childY = currentY + height / 2f

        layoutSubtree(
            node = child,
            parentX = nodeX,
            parentY = nodeY,
            startX = nodeX + direction * X_GAP,
            startY = childY,
            direction = direction,
            outList = outList
        )
        currentY += height + Y_GAP
    }
}

/**
 * Evaluates subtree heights occupied in the layout grid depending on folds.
 */
fun getSubtreeHeight(node: MindMapNode): Float {
    if (node.folded || node.children.isEmpty()) {
        return 65f // basic single card visual spacing height
    }
    val Y_GAP = 30f
    return node.children.map { getSubtreeHeight(it) }.sum() + (node.children.size - 1) * Y_GAP
}

/**
 * Freeplane builtin icon identifiers mapped to gorgeous emojis.
 */
fun emojiOfIcon(builtinName: String): String {
    return when (builtinName.lowercase()) {
        "help", "query" -> "❓"
        "bookmark" -> "🔖"
        "idea", "lightbulb" -> "💡"
        "list" -> "📝"
        "yes", "check", "button_ok" -> "✅"
        "no", "cross", "button_cancel" -> "❌"
        "message", "mail" -> "✉️"
        "star", "bookmark_gold" -> "⭐"
        "warning", "danger" -> "⚠️"
        "clock", "time" -> "⏰"
        "flag" -> "🚩"
        "arrow_right" -> "➡️"
        "arrow_left" -> "⬅️"
        "lock" -> "🔒"
        "unlock" -> "🔓"
        "home" -> "🏠"
        "calendar" -> "📅"
        "folder" -> "📁"
        "desktop", "computer" -> "💻"
        "phone", "mobile" -> "📱"
        "gear" -> "⚙️"
        else -> "🔹"
    }
}

/**
 * Parse standard CSS Hex strings safely without crash.
 */
fun safeParseColor(hexStr: String): Color {
    return try {
        val cleaned = hexStr.trim().removePrefix("#")
        if (cleaned.length == 6) {
            Color(android.graphics.Color.parseColor("#$cleaned"))
        } else if (cleaned.length == 8) {
            Color(android.graphics.Color.parseColor("#$cleaned"))
        } else {
            Color(0xFF3B82F6) // Default M3 primary blue fallback
        }
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }
}
