package kr.co.root.legolas.location.tracking

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.co.root.legolas.location.data.LocationUploader

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocationUploadEntryPoint {
    fun uploader(): LocationUploader
}
