import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.asdevs.kinematix.R
import com.asdevs.kinematix.postureCorrection.ExerciseAnalyzer
import com.asdevs.kinematix.models.ExerciseType
import com.asdevs.kinematix.postureCorrection.GraphicOverlay
import com.asdevs.kinematix.postureCorrection.PoseGraphic
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PostureAIFragment : Fragment() {
    private lateinit var previewView: PreviewView
    private lateinit var graphicOverlay: GraphicOverlay
    private lateinit var postureInfoText: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var currentDialog: AlertDialog? = null
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var exerciseTypeDropdown: AutoCompleteTextView
    private var currentExerciseType: ExerciseType = ExerciseType.POSTURE
    private lateinit var repCountText: TextView
    private lateinit var resetButton: ImageButton


    private lateinit var poseDetector: PoseDetector
    private lateinit var exerciseAnalyzer: ExerciseAnalyzer

    private val exerciseCategories = mapOf(
        "Strength Training" to listOf(
            "Squat Form" to ExerciseType.SQUAT,
            "Deadlift Form" to ExerciseType.DEADLIFT,
            "Bench Press Form" to ExerciseType.BENCH_PRESS,
            "Shoulder Press" to ExerciseType.SHOULDER_PRESS,
            "Bicep Curl" to ExerciseType.BICEP_CURL,
            "Tricep Extension" to ExerciseType.TRICEP_EXTENSION,
            "Plank" to ExerciseType.PLANK
        ),
        "Yoga" to listOf(
            "Warrior Pose" to ExerciseType.WARRIOR_POSE,
            "Tree Pose" to ExerciseType.TREE_POSE,
            "Downward Dog" to ExerciseType.DOWNWARD_DOG,
            "Cobra Pose" to ExerciseType.COBRA_POSE,
            "Chair Pose" to ExerciseType.CHAIR_POSE
        ),
        "Posture" to listOf(
            "Posture Check" to ExerciseType.POSTURE
        )
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                showPermissionDeniedDialog()
            }
        }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val TAG = "PostureAIFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_posture_ai, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        previewView = view.findViewById(R.id.previewView)
        graphicOverlay = view.findViewById(R.id.graphicOverlay)
        postureInfoText = view.findViewById(R.id.postureInfoText)
        categoryDropdown = view.findViewById(R.id.category_auto_complete)
        exerciseTypeDropdown = view.findViewById(R.id.exercise_type_auto_complete)


        view.findViewById<ImageButton>(R.id.closeButton).setOnClickListener {
            stopCamera()
            requireActivity().onBackPressed()
        }

        setupCamera()
        setupExerciseSelections()
        checkCameraPermission()

    }


    private fun setupCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        poseDetector = PoseDetection.getClient(options)

        exerciseAnalyzer = ExerciseAnalyzer(ExerciseType.POSTURE)
    }

    private fun setupExerciseSelections() {
        if (!isAdded) return

        try {
            // Setup Category Dropdown
            val categoryAdapter = ArrayAdapter(
                requireContext(),
                R.layout.dropdown_item,
                exerciseCategories.keys.toList()
            )

            categoryDropdown.apply {
                setAdapter(categoryAdapter)
                setText(exerciseCategories.keys.first(), false)

                setOnItemClickListener { _, _, position, _ ->
                    if (!isAdded) return@setOnItemClickListener
                    val selectedCategory = exerciseCategories.keys.toList()[position]
                    setupExerciseDropdown(exerciseCategories[selectedCategory] ?: emptyList())
                }
            }

            // Initial setup with first category
            setupExerciseDropdown(exerciseCategories[exerciseCategories.keys.first()] ?: emptyList())

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up exercise selections: ${e.message}")
        }
    }

    private fun setupExerciseDropdown(exercises: List<Pair<String, ExerciseType>>) {
        if (!isAdded) return

        try {
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.dropdown_item,
                exercises.map { it.first }
            )

            exerciseTypeDropdown.apply {
                setAdapter(adapter)
                setText(exercises.first().first, false)

                setOnItemClickListener { _, _, position, _ ->
                    if (!isAdded) return@setOnItemClickListener
                    currentExerciseType = exercises[position].second
                    exerciseAnalyzer = ExerciseAnalyzer(currentExerciseType)
                    updateExerciseHints()
                }
            }

            // Set initial exercise type
            currentExerciseType = exercises.first().second
            exerciseAnalyzer = ExerciseAnalyzer(currentExerciseType)
            updateExerciseHints()

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up exercise dropdown: ${e.message}")
        }
    }


    private fun updateExerciseHints() {
        val hintText = when (currentExerciseType) {
            ExerciseType.POSTURE -> "Stand straight and face the camera"
            ExerciseType.SQUAT -> "Stand sideways to the camera"
            ExerciseType.DEADLIFT -> "Stand sideways to the camera"
            ExerciseType.BENCH_PRESS -> "Ensure your bench setup is visible"
            ExerciseType.SHOULDER_PRESS -> "Stand facing the camera, shoulders visible"
            ExerciseType.BICEP_CURL -> "Stand facing the camera, arms visible"
            ExerciseType.TRICEP_EXTENSION -> "Stand sideways to the camera"
            ExerciseType.WARRIOR_POSE -> "Stand with your side to the camera"
            ExerciseType.TREE_POSE -> "Stand facing the camera"
            ExerciseType.DOWNWARD_DOG -> "Ensure full body is visible from the side"
            ExerciseType.COBRA_POSE -> "Ensure full body is visible from the side"
            ExerciseType.CHAIR_POSE -> "Stand facing the camera"
            ExerciseType.PLANK -> "Ensure full body is visible from the side"
        }

        postureInfoText.text = hintText
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted, start camera
                Log.d(TAG, "Camera permission already granted")
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show an explanation to the user
                Log.d(TAG, "Showing permission explanation dialog")
                showPermissionExplanationDialog()
            }
            else -> {
                // Request the permission
                Log.d(TAG, "Requesting camera permission")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        try {
            currentDialog?.dismiss()
            currentDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Camera Permission Required")
                .setMessage("This feature needs camera access to analyze your posture. Please grant camera permission to continue.")
                .setPositiveButton("Grant Access") { dialog, _ ->
                    dialog.dismiss()
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Camera permission is required for posture analysis",
                        Toast.LENGTH_LONG
                    ).show()
                    requireActivity().onBackPressed()
                }
                .setCancelable(false)
                .create()

            currentDialog?.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing permission dialog: ${e.message}")
            Toast.makeText(
                requireContext(),
                "Error requesting camera permission",
                Toast.LENGTH_LONG
            ).show()
            requireActivity().onBackPressed()
        }
    }

    private fun showPermissionDeniedDialog() {
        currentDialog?.dismiss()
        currentDialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog_Dark)
            .setTitle("Permission Denied")
            .setMessage("This feature cannot work without camera access. Please enable camera permission in app settings to use this feature.")
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                requireActivity().onBackPressed()
            }
            .setCancelable(false)
            .create()  // Add .create() here

        currentDialog?.show()
    }

    fun dismissDialogs() {
        currentDialog?.dismiss()
        currentDialog = null
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Camera permission is required for posture analysis",
                        Toast.LENGTH_LONG
                    ).show()
                    activity?.onBackPressed()
                }
            }
        }
    }

    private var isCameraRunning = false

    private fun startCamera() {
        if (!isAdded || isCameraRunning)  return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                if(!isAdded) return@addListener

                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()

                preview.setSurfaceProvider(previewView.surfaceProvider)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                // Add the image analyzer
                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                try {
                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        viewLifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )

                    previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewView.scaleType = PreviewView.ScaleType.FIT_CENTER

                } catch (e: Exception) {
                    Log.e(TAG, "Use case binding failed", e)
                    Toast.makeText(requireContext(), "Failed to start camera", Toast.LENGTH_SHORT).show()
                }

                isCameraRunning = true

            } catch (e: Exception) {
                Log.e(TAG, "Camera provider is unavailable", e)
                Toast.makeText(requireContext(), "Camera is unavailable", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImage(imageProxy: ImageProxy) {
        if (!isAdded) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    try {
                        // Check if fragment is still attached
                        if (!isAdded || view == null) {
                            return@addOnSuccessListener
                        }

                        graphicOverlay.clear()

                        // Analyze pose and get feedback
                        val feedback = exerciseAnalyzer.analyzePose(pose)

                        // Update UI using view's post method instead of activity
                        view?.post {
                            try {
                                if (isAdded && view != null) {
                                    postureInfoText.text = feedback.message

                                    // Draw pose
                                    val poseGraphic = PoseGraphic(
                                        graphicOverlay,
                                        pose,
                                        currentExerciseType,
                                        showInFrameLikelihood = false,
                                        isGoodPosture = feedback.isCorrectForm
                                    )
                                    graphicOverlay.add(poseGraphic)
                                    graphicOverlay.postInvalidate()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating UI: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing pose: ${e.message}")
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        Log.e(TAG, "Pose detection failed", e)
                    }
                }
                .addOnCompleteListener {
                    try {
                        imageProxy.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error closing image proxy: ${e.message}")
                    }
                }
        } else {
            imageProxy.close()
        }
    }

    override fun onDestroyView() {
        try {
            isCameraRunning = false
            dismissDialogs()
            cameraExecutor.shutdown()
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroyView: ${e.message}")
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
        try {
            isCameraRunning=false
            cameraExecutor.shutdown()
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}")
        }
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        try {
            isCameraRunning = false
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStop: ${e.message}")
        }
    }

    override fun onDetach() {
        try {
            isCameraRunning = false
            cameraExecutor.shutdown()
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDetach: ${e.message}")
        }
        super.onDetach()
    }

    private fun stopCamera() {
        try {
            isCameraRunning = false
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera: ${e.message}")
        }
    }


}