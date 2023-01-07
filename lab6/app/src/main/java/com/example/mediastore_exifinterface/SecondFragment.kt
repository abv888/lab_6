package com.example.mediastore_exifinterface

import android.annotation.SuppressLint
import android.app.Activity
import android.app.RecoverableSecurityException
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.exifinterface.media.ExifInterface
import androidx.navigation.fragment.findNavController
import com.example.mediastore_exifinterface.databinding.FragmentSecondBinding
import java.io.IOException

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val contentResolver = requireContext().contentResolver

        /* Загружаем фотографию */
        binding.imageViewEdit.setImageURI(FirstFragment.uri)

        /* Открывает uri в потоке и через него добираемся до Exif */
        contentResolver.openInputStream(FirstFragment.uri!!)?.use { stream ->
            val exif = ExifInterface(stream)
            binding.apply {
                /* И тут получаем нужные нам данные */
                creationDataEt.setText(exif.getAttribute(ExifInterface.TAG_DATETIME) ?: "")
                latitudeEt.setText(returnCoordinate(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)) + ' ' +
                        (exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) ?: ""))
                if (latitudeEt.text.isBlank()) latitudeEt.setText("")
                longitudeEt.setText(returnCoordinate(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)) + ' ' +
                        (exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF) ?: ""))
                if (longitudeEt.text.isBlank()) longitudeEt.setText("")
                creationDeviceEt.setText(exif.getAttribute(ExifInterface.TAG_MAKE) ?: "")
                modelCreationDeviceEt.setText(exif.getAttribute(ExifInterface.TAG_MODEL) ?: "")
            }
        }

        binding.saveChangeBtn.setOnClickListener {
            try {
                /* По нажатию на кнопку сохраняем данные */
                saveChanges()
            } catch (securityException: SecurityException) {
                val recoverableSecurityException =
                    securityException as? RecoverableSecurityException
                        ?: throw securityException
                /* Если нет разрешения, то просим разрешения у пользователя */
                val intentSender = recoverableSecurityException.userAction.actionIntent.intentSender
                requestUri.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /* Функция для обработки данных координат в пользовательский вид */
    private fun returnCoordinate(attribute: String?): String {
        return if (attribute == null) {
            ""
        } else {
            val coor = attribute.split("/1,")
            val angel = coor[0]
            val minute = coor[1]
            val secCoor = coor[2].split("/")
            val second = secCoor[0].toDouble() / secCoor[1].toDouble()
            "$angel°${minute}'${"%.1f".format(second).replace(",", ".")}\""
        }
    }

    /* Если разрешение полученно, то сохраняем данные */
    private var requestUri = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result != null && result.resultCode == Activity.RESULT_OK) {
            saveChanges()
        }
    }

    private fun saveChanges() {
        val contentResolver = requireContext().contentResolver
        /* Тут также через поток, только записываем данные и сохраняем их */
        contentResolver.openFileDescriptor(FirstFragment.uri!!, "rw")?.use { fileDescriptor ->
            val exif = ExifInterface(fileDescriptor.fileDescriptor)
            binding.apply {
                exif.setAttribute(ExifInterface.TAG_DATETIME, creationDataEt.text.toString())
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, saveCoordinate(latitudeEt.text.toString())[0])
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, saveCoordinate(latitudeEt.text.toString())[1])
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, saveCoordinate(longitudeEt.text.toString())[0])
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, saveCoordinate(longitudeEt.text.toString())[1])
                exif.setAttribute(ExifInterface.TAG_MAKE, creationDeviceEt.text.toString())
                exif.setAttribute(ExifInterface.TAG_MODEL, modelCreationDeviceEt.text.toString())
            }
            exif.saveAttributes()
        }
        /* И переходим на первый фрагмент */
        findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
    }

    /* Функция для обработки данных координат в google вид */
    private fun saveCoordinate(attribute: String): List<String> {
        return if (attribute.isBlank()) {
            listOf("", "")
        } else {
            val angel = attribute.substring(0 until attribute.indexOf('°'))
            val minute = attribute.substring(attribute.indexOf('°') + 1 until attribute.indexOf('\''))
            val second = attribute.substring(attribute.indexOf('\'') + 1 until attribute.indexOf('\"'))
            listOf("$angel/1,$minute/1,$second/10000", attribute.substring(attribute.length - 1))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}