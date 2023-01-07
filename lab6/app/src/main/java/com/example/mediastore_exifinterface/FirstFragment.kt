package com.example.mediastore_exifinterface

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mediastore_exifinterface.databinding.FragmentFirstBinding

/** The request code for requesting [Manifest.permission.READ_EXTERNAL_STORAGE] permission. */
private const val READ_EXTERNAL_STORAGE_REQUEST = 0x1045

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    companion object {
        var uri: Uri? = null
    }

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Если есть uri, то надо загрузить данные фотографии */
        uri?.let { loadingData(it) }

        /* По нажатию на кнопку проверяем, есть ли разрешение... */
        binding.imageSelectionBtn.setOnClickListener {
            if (haveStoragePermission()) {
                /* Если есть разрешение, то выбираем фотографию */
                pickImage()
            } else {
                /* Если нет, то надо открыть окно, чтоб пользователь предоставил разрешение */
                requestPermission()
            }
        }

        /* При нажатии переходит в окно редактирования данных */
        binding.editBtn.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /* Если ли разрешение */
    private fun haveStoragePermission() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PERMISSION_GRANTED

    /* Открываем окно для предоставления разрешения */
    private fun requestPermission() {
        if (!haveStoragePermission()) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            ActivityCompat.requestPermissions(requireActivity(), permissions, READ_EXTERNAL_STORAGE_REQUEST)
        }
    }


    private fun pickImage() {
        /* Настраиваем активити */
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "image/*"

        /* Запускаем активити */
        requestUri.launch(intent)
    }

    private var requestUri = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        /* Если выбрали фотографию, то ... */
        if (result != null && result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                intent.data?.let { fileUri ->
                    /* Загружаем данные */
                    loadingData(fileUri)
                    /* И сохраняем uri, чтоб знать, было уже фото или нет*/
                    uri = fileUri
                }
            }
        }
    }

    /* Функция для обработки данных координат */
    private fun returnCoordinate(attribute: String?): String {
        return if (attribute == null) {
            "No data"
        } else {
            val coor = attribute.split("/1,")
            val angel = coor[0]
            val minute = coor[1]
            val secCoor = coor[2].split("/")
            val second = secCoor[0].toDouble() / secCoor[1].toDouble()
            "$angel°${minute}'${"%.1f".format(second).replace(",", ".")}\""
        }
    }

    private fun returnData(attribute: String?): String {
        return attribute ?: "No data"
    }

    @SuppressLint("SetTextI18n")
    private fun loadingData(uriImg: Uri) {
        val contentResolver = requireContext().contentResolver
        /* Делаем видимыми элементы */
        binding.constraintLayout.visibility = View.VISIBLE

        /* Загружаем фотографию */
        binding.imageView.setImageURI(uriImg)

        /* Открывает uri в потоке и через него добираемся до Exif */
        contentResolver.openInputStream(uriImg)?.use { stream ->
            val exif = ExifInterface(stream)
            binding.apply {
                /* И тут получаем нужные нам данные */
                creationDateTv.text = returnData(exif.getAttribute(ExifInterface.TAG_DATETIME))
                latitudeTv.text = returnCoordinate(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)) + ' ' +
                        (exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) ?: "")
                longitudeTv.text = returnCoordinate(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)) + ' ' +
                        (exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF) ?: "")
                creationDeviceTv.text = returnData(exif.getAttribute(ExifInterface.TAG_MAKE))
                modelCreationDeviceTv.text = returnData(exif.getAttribute(ExifInterface.TAG_MODEL))
            }
        }
    }
}