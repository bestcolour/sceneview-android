package io.github.sceneview.sample.armodelviewer.compose

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import io.github.sceneview.sample.SceneviewTheme


private const val kModelFile = "models/Air Squat.glb"
private const val scale = 0.02f
//private const val kModelOffsetScale = -0.25f
class MainActivity : ComponentActivity() {

    private var parentNode: Node? = null
    private var modelNode: AnchorNode? = null
//    private var previousX: Float = 0f  // Track previous X position for swipe

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SceneviewTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val engine = rememberEngine()
                    val modelLoader = rememberModelLoader(engine)
                    val materialLoader = rememberMaterialLoader(engine)
                    val cameraNode = rememberARCameraNode(engine)
                    val childNodes = rememberNodes()
                    val view = rememberView(engine)
                    val collisionSystem = rememberCollisionSystem(view)

                    var planeRenderer by remember { mutableStateOf(true) }
                    var trackingFailureReason by remember { mutableStateOf<TrackingFailureReason?>(null) }
                    var frame by remember { mutableStateOf<Frame?>(null) }

                    ARScene(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {

                                // Detect drag gestures for rotation
                                detectHorizontalDragGestures { change, dragAmount ->
                                    val deltaX = dragAmount  // Horizontal drag movement
                                    rotateParentNode(deltaX)
                                }
                            },
                        childNodes = childNodes,
                        engine = engine,
                        view = view,
                        modelLoader = modelLoader,
                        collisionSystem = collisionSystem,
                        sessionConfiguration = { session, config ->
                            config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                true -> Config.DepthMode.AUTOMATIC
                                else -> Config.DepthMode.DISABLED
                            }
                            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                        },
                        cameraNode = cameraNode,
                        planeRenderer = planeRenderer,
                        onTrackingFailureChanged = { trackingFailureReason = it },
                        onSessionUpdated = { session, updatedFrame ->
                            frame = updatedFrame
                            if (childNodes.isEmpty()) {
                                updatedFrame.getUpdatedPlanes()
                                    .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                                    ?.let { it.createAnchorOrNull(it.centerPose) }
                                    ?.let { anchor ->
                                        childNodes += createAnchorNode(engine, modelLoader, materialLoader, anchor)
                                    }
                            }
                        }
                        ,
                        onGestureListener = rememberOnGestureListener(
                            onSingleTapConfirmed = { motionEvent, node ->
                                if (node == null) {
                                    val hitResults = frame?.hitTest(motionEvent.x, motionEvent.y)
                                    hitResults?.firstOrNull {
                                        it.isValid(depthPoint = false, point = false)
                                    }?.createAnchorOrNull()
                                        ?.let { anchor ->
                                            planeRenderer = false
                                            modelNode?.let {
                                                it.parent?.removeChildNode(it)
                                                it.anchor.detach()
                                                modelNode = null
                                            }

                                            modelNode = createAnchorNode(engine, modelLoader, materialLoader, anchor)

                                            parentNode?.let {
//                                                val v = offsetPosition(modelNode!!.position, modelNode!!.transform.forward)
////                                                 set parent node pos to modelNode curr pos
//                                                it.position = v


                                                it.position = modelNode!!.position

                                                Log.d("Instantiate Model Node", "parentNode.position ${it.position} modelNode.position ${modelNode!!.position}")

                                                // set model node parent
                                                it.addChildNode(modelNode!!)
                                                Log.d("Instantiate Model Node", "parentNode.childNodes.size=  ${it.childNodes.size}")

                                            }


//                                            modelNode?.let {
//                                                childNodes += it
//                                            }
                                        }
                                }
                            }
                        )
                    )

                    // create only once
                    if (parentNode ==null)
                    {
                        parentNode = Node(engine = engine)
                        parentNode?.let {
                            childNodes += it
                        }
                    }


                    Text(
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp, start = 32.dp, end = 32.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 28.sp,
                        color = Color.White,
                        text = trackingFailureReason?.getDescription(LocalContext.current) ?: if (childNodes.isEmpty()) {
                            stringResource(R.string.point_your_phone_down)
                        } else {
                            stringResource(R.string.tap_anywhere_to_add_model)
                        }
                    )
                }
            }
        }
    }

    // Function to handle rotation based on horizontal drag movement
    private fun rotateParentNode(deltaX: Float) {

        parentNode?.let {
            Log.d("RotateParent", "Outside it.childnode.size: ${parentNode!!.childNodes.size}")

            parentNode.apply {
                val rotationSpeed = 0.5f  // Adjust the speed of rotation

                // Update only the Y-axis rotation, keeping X and Z the same
                it.rotation = it.rotation.copy(
                    y = it.rotation.y + deltaX * rotationSpeed
                )

                Log.d("RotateParent", "Inside it.rotation: ${it.rotation}")
                Log.d("RotateParent", "Inside it.childnode.size: ${it.childNodes.size}")

            }
        }
    }

//    private  fun offsetPosition(pos:Position, forwardVector:Float3) : Float3
//    {
//        val offsetPosition = Float3(
//            pos.x + forwardVector.x *kModelOffsetScale,
//            pos.y + forwardVector.y*kModelOffsetScale,
//            pos.z + forwardVector.z * kModelOffsetScale
//        )
//        return offsetPosition;
//    }

    fun createAnchorNode(
        engine: Engine,
        modelLoader: ModelLoader,
        materialLoader: MaterialLoader,
        anchor: Anchor
    ): AnchorNode {
        val anchorNode = AnchorNode(engine = engine, anchor = anchor)
        val modelNode = ModelNode(
            modelInstance = modelLoader.createModelInstance(kModelFile),
            scaleToUnits = scale
        ).apply {
            isEditable = true
            isRotationEditable = true
            editableScaleRange = 0.2f..0.75f
        }
        val boundingBoxNode = CubeNode(
            engine,
            size = modelNode.extents,
            center = modelNode.center,
            materialInstance = materialLoader.createColorInstance(Color.White.copy(alpha = 0.5f))
        ).apply {
            isVisible = false
        }
        modelNode.addChildNode(boundingBoxNode)
        anchorNode.addChildNode(modelNode)

        listOf(modelNode, anchorNode).forEach {
            it.onEditingChanged = { editingTransforms ->
                boundingBoxNode.isVisible = editingTransforms.isNotEmpty()
            }
        }
        return anchorNode
    }
}
