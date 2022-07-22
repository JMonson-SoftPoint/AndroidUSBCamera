package com.jiangdg.demo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.MultiCameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.demo.databinding.FragmentMultiCameraBinding

/** Multi-road camera demo
 *
 * @author Created by jiangdg on 2022/7/20
 */
class DemoMultiCameraFragment: MultiCameraFragment() {
    private lateinit var mAdapter: CameraAdapter
    private lateinit var mViewBinding: FragmentMultiCameraBinding
    private val mCameraList by lazy {
        ArrayList<MultiCameraClient.Camera> ()
    }

    override fun onCameraAttached(camera: MultiCameraClient.Camera) {
        mCameraList.add(camera)
        mAdapter.notifyItemChanged(mCameraList.size - 1)
        mViewBinding.multiCameraTip.visibility = View.GONE
    }

    override fun onCameraDetached(camera: MultiCameraClient.Camera) {
        for ((position, cam) in mCameraList.withIndex()) {
            if (cam.getUsbDevice().deviceId == cam.getUsbDevice().deviceId) {
                camera.closeCamera()
                mCameraList.removeAt(position)
                mAdapter.notifyItemChanged(position)
                break
            }
        }
        if (mCameraList.isEmpty()) {
            mViewBinding.multiCameraTip.visibility = View.VISIBLE
        }
    }

    override fun onCameraConnected(camera: MultiCameraClient.Camera) {
        for ((position, cam) in mAdapter.data.withIndex()) {
            if (cam.getUsbDevice().deviceId == cam.getUsbDevice().deviceId) {
                mAdapter.notifyItemChanged(position, "switch")
                break
            }
        }
    }

    override fun onCameraDisConnected(camera: MultiCameraClient.Camera) {
        for ((position, cam) in mAdapter.data.withIndex()) {
            if (cam.getUsbDevice().deviceId == cam.getUsbDevice().deviceId) {
                mAdapter.notifyItemChanged(position, "switch")
                break
            }
        }
    }

    override fun initView() {
        super.initView()
        openDebug(true)
        mAdapter = CameraAdapter()
        mAdapter.setNewData(mCameraList)
        mAdapter.bindToRecyclerView(mViewBinding.multiCameraRv)
        mViewBinding.multiCameraRv.adapter = mAdapter
        mViewBinding.multiCameraRv.layoutManager = GridLayoutManager(requireContext(), 2)
        mAdapter.setOnItemChildClickListener { adapter, view, position ->
            val camera = adapter.data[position] as MultiCameraClient.Camera
            when(view.id) {
//                R.id.multi_camera_switch -> {
//                    if (camera.isCameraOpened()) {
//                        camera.closeCamera()
//                        return@setOnItemChildClickListener
//                    }
//                    if (! hasPermission(camera.getUsbDevice())) {
//                        requestPermission(camera.getUsbDevice())
//                        return@setOnItemChildClickListener
//                    }
//                    val textureView = mAdapter.getViewByPosition(position, R.id.multi_camera_texture_view)
//                    camera.openCamera(textureView, getCameraRequest(), object : ICameraStateCallBack {
//                        override fun onState(code: ICameraStateCallBack.State, msg: String?) {
//                            if (code == ICameraStateCallBack.State.ERROR) {
//                                ToastUtils.show(msg ?: "open camera failed.")
//                            }
//                            mAdapter.notifyItemChanged(position, "switch")
//                        }
//                    })
//                }
                R.id.multi_camera_capture_image -> {
                    camera.captureImage(object : ICaptureCallBack {
                        override fun onBegin() {}

                        override fun onError(error: String?) {
                            ToastUtils.show(error ?: "capture image failed")
                        }

                        override fun onComplete(path: String?) {
                            ToastUtils.show(path ?: "capture image success")
                        }
                    })
                }
                R.id.multi_camera_capture_video -> {
                    if (camera.isRecordVideo()) {
                        camera.captureVideoStop()
                        return@setOnItemChildClickListener
                    }
                    camera.captureVideoStart(object : ICaptureCallBack {
                        override fun onBegin() {
                            mAdapter.notifyItemChanged(position, "video")
                        }

                        override fun onError(error: String?) {
                            mAdapter.notifyItemChanged(position, "video")
                            ToastUtils.show(error ?: "capture video failed")
                        }

                        override fun onComplete(path: String?) {
                            mAdapter.notifyItemChanged(position, "video")
                            ToastUtils.show(path ?: "capture video success")
                        }
                    })
                }
                else -> {}
            }
        }
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        mViewBinding = FragmentMultiCameraBinding.inflate(inflater, container, false)
        return mViewBinding.root
    }

    private fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setPreviewWidth(640)
            .setPreviewHeight(480)
            .create()
    }

    inner class CameraAdapter: BaseQuickAdapter<MultiCameraClient.Camera, BaseViewHolder>(R.layout.layout_item_camera) {
        override fun convert(helper: BaseViewHolder, camera: MultiCameraClient.Camera?) {}

        override fun convertPayloads(
            helper: BaseViewHolder,
            camera: MultiCameraClient.Camera?,
            payloads: MutableList<Any>
        ) {
            camera ?: return
            helper.setText(R.id.multi_camera_name, camera.getUsbDevice().deviceName)
            helper.addOnClickListener(R.id.multi_camera_switch)
            helper.addOnClickListener(R.id.multi_camera_capture_video)
            helper.addOnClickListener(R.id.multi_camera_capture_image)
            if (payloads.isEmpty()) {
                return
            }
            // local update
            val switchIv = helper.getView<ImageView>(R.id.multi_camera_switch)
            val captureVideoIv = helper.getView<ImageView>(R.id.multi_camera_capture_video)
            val textureView = helper.getView<AspectRatioTextureView>(R.id.multi_camera_texture_view)
            if (payloads.find { "switch" == it }!=null) {
                if (camera.isCameraOpened()) {
                    camera.closeCamera()
                } else {
                    camera.openCamera(textureView, getCameraRequest(), object : ICameraStateCallBack {
                        override fun onState(code: ICameraStateCallBack.State, msg: String?) {
                            when (code) {
                                ICameraStateCallBack.State.OPENED -> {
                                    switchIv.setImageResource(R.mipmap.ic_switch_on)
                                }
                                ICameraStateCallBack.State.CLOSED -> {
                                    switchIv.setImageResource(R.mipmap.ic_switch_off)
                                }
                                else -> {
                                    ToastUtils.show(msg ?: "open camera failed.")
                                    switchIv.setImageResource(R.mipmap.ic_switch_off)
                                }
                            }
                        }
                    })
                }
            }
            if (payloads.find { "video" == it }!=null) {
                if (camera.isRecordVideo()) {
                    captureVideoIv.setImageResource(R.mipmap.ic_capture_video_on)
                } else {
                    captureVideoIv.setImageResource(R.mipmap.ic_capture_video_off)
                }
            }
        }
    }
}