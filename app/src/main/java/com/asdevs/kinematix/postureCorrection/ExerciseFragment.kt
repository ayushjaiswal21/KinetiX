package com.asdevs.kinematix.postureCorrection

import ExerciseGraphic
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.asdevs.kinematix.R
import com.asdevs.kinematix.models.ExerciseType
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ExerciseFragment : Fragment() {
    private lateinit var previewView: PreviewView
    private lateinit var graphicOverlay: GraphicOverlay
    private lateinit var exerciseAnalyzer: ExerciseAnalyzer
    private lateinit var repCounter: RepCounter
    private lateinit var repCountText: TextView
    private lateinit var feedbackText: TextView
    private lateinit var resetButton: Button
    private lateinit var poseDetector: PoseDetector
    private lateinit var cameraExecutor: ExecutorService

    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var processingImage = false
    private var isFirstFrame = true
    private var lastProcessTime = 0L
    private val PROCESS_INTERVAL = 100L // Process every 100ms

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupCamera()
        } else {
            showPermissionDeniedMessage()
        }
    }

    private val exerciseType: ExerciseType by lazy {
        arguments?.getString(ARG_EXERCISE_TYPE)?.let {
            ExerciseType.valueOf(it)
        } ?: ExerciseType.SQUAT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exerciseAnalyzer = ExerciseAnalyzer(exerciseType)
        repCounter = RepCounter(exerciseType)

        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .setPreferredHardwareConfigs(PoseDetectorOptions.CPU_GPU)
            .build()
        poseDetector = PoseDetection.getClient(options)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exercise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        checkCameraPermission()
    }

    private fun initializeViews(view: View) {
        previewView = view.findViewById(R.id.previewView)
        graphicOverlay = view.findViewById(R.id.graphicOverlay)
        repCountText = view.findViewById(R.id.repCountText)
        feedbackText = view.findViewById(R.id.feedbackText)
        resetButton = view.findViewById(R.id.resetButton)

        initializeDefaultText()
    }

    private fun initializeDefaultText() {
        repCountText.text = getString(R.string.rep_count_format, 0)
        feedbackText.text = getString(R.string.waiting_for_pose)
        feedbackText.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_form))
    }

    private fun checkCameraPermission() {
        when {
            hasCameraPermission() -> setupCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> showPermissionRationale()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionRationale() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(context, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
        activity?.onBackPressed()
    }

    @SuppressLint("RestrictedApi")
    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Create Preview use case
                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setTargetRotation(previewView.display.rotation)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Create Image Analysis use case
                imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setTargetRotation(previewView.display.rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy -> processImage(imageProxy) }
                    }

                // Select front camera
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                try {
                    // Unbind any existing use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    camera = cameraProvider.bindToLifecycle(
                        viewLifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )

                    // Update graphic overlay dimensions
                    imageAnalyzer?.attachedSurfaceResolution?.let { resolution ->
                        graphicOverlay.setCameraInfo(
                            resolution.width,
                            resolution.height,
                            true // Image is flipped for front camera
                        )
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Use case binding failed", e)
                    showCameraError()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Camera provider is not available", e)
                showCameraError()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val preview = createPreviewUseCase()
        imageAnalyzer = createImageAnalyzer()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            updateGraphicOverlaySize()
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
            showCameraError()
        }
    }

    private fun createPreviewUseCase() = Preview.Builder()
        .build()
        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

    private fun createImageAnalyzer() = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setTargetRotation(previewView.display.rotation)
        .build()
        .also {
            it.setAnalyzer(cameraExecutor) { imageProxy -> processImage(imageProxy) }
        }

    @SuppressLint("RestrictedApi")
    private fun updateGraphicOverlaySize() {
        previewView.post {
            val previewSize = imageAnalyzer?.attachedSurfaceResolution ?: return@post
            graphicOverlay.setCameraInfo(previewSize.width, previewSize.height, true)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        if (shouldSkipFrame()) {
            imageProxy.close()
            return
        }

        processingImage = true
        lastProcessTime = System.currentTimeMillis()

        imageProxy.image?.let { mediaImage ->
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            processPoseDetection(image, imageProxy)
        } ?: run {
            processingImage = false
            imageProxy.close()
        }
    }

    private fun shouldSkipFrame(): Boolean {
        val currentTime = System.currentTimeMillis()
        return processingImage || (!isFirstFrame && currentTime - lastProcessTime < PROCESS_INTERVAL)
    }

    private fun processPoseDetection(image: InputImage, imageProxy: ImageProxy) {
        poseDetector.process(image)
            .addOnSuccessListener { pose ->
                isFirstFrame = false
                analyzePose(pose)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Pose detection failed", e)
            }
            .addOnCompleteListener {
                processingImage = false
                imageProxy.close()
            }
    }

    private fun analyzePose(pose: Pose) {
        if (!isAdded) return

        updateRepCount(pose)
        updateFormFeedback(pose)
        updatePoseVisualization(pose)
    }

    private fun updateRepCount(pose: Pose) {
        val reps = repCounter.processPosition(pose)
        activity?.runOnUiThread {
            repCountText.text = getString(R.string.rep_count_format, reps)
        }
    }

    private fun updateFormFeedback(pose: Pose) {
        val feedback = exerciseAnalyzer.analyzePose(pose)
        activity?.runOnUiThread {
            feedbackText.text = feedback.message
            feedbackText.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (feedback.isCorrectForm) R.color.good_form else R.color.bad_form
                )
            )
        }
    }

    private fun updatePoseVisualization(pose: Pose) {
        graphicOverlay.clear()
        // Add both PoseGraphic and ExerciseGraphic
        graphicOverlay.add(PoseGraphic(
            graphicOverlay,
            pose,
            exerciseType,
            showInFrameLikelihood = true,
            isGoodPosture = true
        ))
        graphicOverlay.add(ExerciseGraphic(graphicOverlay, pose, exerciseType))
        graphicOverlay.invalidate() // Force redraw
    }

    private fun showCameraError() {
        Toast.makeText(context, getString(R.string.camera_initialization_failed), Toast.LENGTH_SHORT).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        poseDetector.close()
    }

    companion object {
        private const val TAG = "ExerciseFragment"
        private const val ARG_EXERCISE_TYPE = "exercise_type"

        fun newInstance(exerciseType: ExerciseType) = ExerciseFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_EXERCISE_TYPE, exerciseType.name)
            }
        }
    }
}