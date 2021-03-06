// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: fileformat.proto

package crosby.binary;

public final class Fileformat {
  private Fileformat() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public static final class Blob extends
      com.google.protobuf.GeneratedMessage {
    // Use Blob.newBuilder() to construct.
    private Blob() {
      initFields();
    }
    private Blob(boolean noInit) {}
    
    private static final Blob defaultInstance;
    public static Blob getDefaultInstance() {
      return defaultInstance;
    }
    
    @Override
	public Blob getDefaultInstanceForType() {
      return defaultInstance;
    }
    
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return crosby.binary.Fileformat.internal_static_Blob_descriptor;
    }
    
    @Override
	protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return crosby.binary.Fileformat.internal_static_Blob_fieldAccessorTable;
    }
    
    // optional bytes raw = 1;
    public static final int RAW_FIELD_NUMBER = 1;
    private boolean hasRaw;
    private com.google.protobuf.ByteString raw_ = com.google.protobuf.ByteString.EMPTY;
    public boolean hasRaw() { return hasRaw; }
    public com.google.protobuf.ByteString getRaw() { return raw_; }
    
    // optional int32 raw_size = 2;
    public static final int RAW_SIZE_FIELD_NUMBER = 2;
    private boolean hasRawSize;
    private int rawSize_ = 0;
    public boolean hasRawSize() { return hasRawSize; }
    public int getRawSize() { return rawSize_; }
    
    // optional bytes zlib_data = 3;
    public static final int ZLIB_DATA_FIELD_NUMBER = 3;
    private boolean hasZlibData;
    private com.google.protobuf.ByteString zlibData_ = com.google.protobuf.ByteString.EMPTY;
    public boolean hasZlibData() { return hasZlibData; }
    public com.google.protobuf.ByteString getZlibData() { return zlibData_; }
    
    // optional bytes lzma_data = 4;
    public static final int LZMA_DATA_FIELD_NUMBER = 4;
    private boolean hasLzmaData;
    private com.google.protobuf.ByteString lzmaData_ = com.google.protobuf.ByteString.EMPTY;
    public boolean hasLzmaData() { return hasLzmaData; }
    public com.google.protobuf.ByteString getLzmaData() { return lzmaData_; }
    
    // optional bytes bzip2_data = 5;
    public static final int BZIP2_DATA_FIELD_NUMBER = 5;
    private boolean hasBzip2Data;
    private com.google.protobuf.ByteString bzip2Data_ = com.google.protobuf.ByteString.EMPTY;
    public boolean hasBzip2Data() { return hasBzip2Data; }
    public com.google.protobuf.ByteString getBzip2Data() { return bzip2Data_; }
    
    private void initFields() {
    }
    @Override
	public final boolean isInitialized() {
      return true;
    }
    
    @Override
	public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (hasRaw()) {
        output.writeBytes(1, getRaw());
      }
      if (hasRawSize()) {
        output.writeInt32(2, getRawSize());
      }
      if (hasZlibData()) {
        output.writeBytes(3, getZlibData());
      }
      if (hasLzmaData()) {
        output.writeBytes(4, getLzmaData());
      }
      if (hasBzip2Data()) {
        output.writeBytes(5, getBzip2Data());
      }
      getUnknownFields().writeTo(output);
    }
    
    private int memoizedSerializedSize = -1;
    @Override
	public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;
    
      size = 0;
      if (hasRaw()) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(1, getRaw());
      }
      if (hasRawSize()) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(2, getRawSize());
      }
      if (hasZlibData()) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(3, getZlibData());
      }
      if (hasLzmaData()) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(4, getLzmaData());
      }
      if (hasBzip2Data()) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(5, getBzip2Data());
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }
    
    public static crosby.binary.Fileformat.Blob parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static crosby.binary.Fileformat.Blob parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static crosby.binary.Fileformat.Blob parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static crosby.binary.Fileformat.Blob parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static crosby.binary.Fileformat.Blob parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static crosby.binary.Fileformat.Blob parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    public static crosby.binary.Fileformat.Blob parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static crosby.binary.Fileformat.Blob parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static crosby.binary.Fileformat.Blob parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static crosby.binary.Fileformat.Blob parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    
    public static Builder newBuilder() { return Builder.create(); }
    @Override
	public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(crosby.binary.Fileformat.Blob prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    @Override
	public Builder toBuilder() { return newBuilder(this); }
    
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> {
      private crosby.binary.Fileformat.Blob result;
      
      // Construct using crosby.binary.Fileformat.Blob.newBuilder()
      private Builder() {}
      
      private static Builder create() {
        Builder builder = new Builder();
        builder.result = new crosby.binary.Fileformat.Blob();
        return builder;
      }
      
      @Override
	protected crosby.binary.Fileformat.Blob internalGetResult() {
        return result;
      }
      
      @Override
	public Builder clear() {
        if (result == null) {
          throw new IllegalStateException(
            "Cannot call clear() after build().");
        }
        result = new crosby.binary.Fileformat.Blob();
        return this;
      }
      
      @Override
	public Builder clone() {
        return create().mergeFrom(result);
      }
      
      @Override
	public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return crosby.binary.Fileformat.Blob.getDescriptor();
      }
      
      @Override
	public crosby.binary.Fileformat.Blob getDefaultInstanceForType() {
        return crosby.binary.Fileformat.Blob.getDefaultInstance();
      }
      
      @Override
	public boolean isInitialized() {
        return result.isInitialized();
      }
      @Override
	public crosby.binary.Fileformat.Blob build() {
        if (result != null && !isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return buildPartial();
      }
      
      private crosby.binary.Fileformat.Blob buildParsed()
          throws com.google.protobuf.InvalidProtocolBufferException {
        if (!isInitialized()) {
          throw newUninitializedMessageException(
            result).asInvalidProtocolBufferException();
        }
        return buildPartial();
      }
      
      @Override
	public crosby.binary.Fileformat.Blob buildPartial() {
        if (result == null) {
          throw new IllegalStateException(
            "build() has already been called on this Builder.");
        }
        crosby.binary.Fileformat.Blob returnMe = result;
        result = null;
        return returnMe;
      }
      
      @Override
	public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof crosby.binary.Fileformat.Blob) {
          return mergeFrom((crosby.binary.Fileformat.Blob)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }
      
      public Builder mergeFrom(crosby.binary.Fileformat.Blob other) {
        if (other == crosby.binary.Fileformat.Blob.getDefaultInstance()) return this;
        if (other.hasRaw()) {
          setRaw(other.getRaw());
        }
        if (other.hasRawSize()) {
          setRawSize(other.getRawSize());
        }
        if (other.hasZlibData()) {
          setZlibData(other.getZlibData());
        }
        if (other.hasLzmaData()) {
          setLzmaData(other.getLzmaData());
        }
        if (other.hasBzip2Data()) {
          setBzip2Data(other.getBzip2Data());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }
      
      @Override
	public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder(
            this.getUnknownFields());
        while (true) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              this.setUnknownFields(unknownFields.build());
              return this;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                this.setUnknownFields(unknownFields.build());
                return this;
              }
              break;
            }
            case 10: {
              setRaw(input.readBytes());
              break;
            }
            case 16: {
              setRawSize(input.readInt32());
              break;
            }
            case 26: {
              setZlibData(input.readBytes());
              break;
            }
            case 34: {
              setLzmaData(input.readBytes());
              break;
            }
            case 42: {
              setBzip2Data(input.readBytes());
              break;
            }
          }
        }
      }
      
      
      // optional bytes raw = 1;
      public boolean hasRaw() {
        return result.hasRaw();
      }
      public com.google.protobuf.ByteString getRaw() {
        return result.getRaw();
      }
      public Builder setRaw(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasRaw = true;
        result.raw_ = value;
        return this;
      }
      public Builder clearRaw() {
        result.hasRaw = false;
        result.raw_ = getDefaultInstance().getRaw();
        return this;
      }
      
      // optional int32 raw_size = 2;
      public boolean hasRawSize() {
        return result.hasRawSize();
      }
      public int getRawSize() {
        return result.getRawSize();
      }
      public Builder setRawSize(int value) {
        result.hasRawSize = true;
        result.rawSize_ = value;
        return this;
      }
      public Builder clearRawSize() {
        result.hasRawSize = false;
        result.rawSize_ = 0;
        return this;
      }
      
      // optional bytes zlib_data = 3;
      public boolean hasZlibData() {
        return result.hasZlibData();
      }
      public com.google.protobuf.ByteString getZlibData() {
        return result.getZlibData();
      }
      public Builder setZlibData(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasZlibData = true;
        result.zlibData_ = value;
        return this;
      }
      public Builder clearZlibData() {
        result.hasZlibData = false;
        result.zlibData_ = getDefaultInstance().getZlibData();
        return this;
      }
      
      // optional bytes lzma_data = 4;
      public boolean hasLzmaData() {
        return result.hasLzmaData();
      }
      public com.google.protobuf.ByteString getLzmaData() {
        return result.getLzmaData();
      }
      public Builder setLzmaData(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasLzmaData = true;
        result.lzmaData_ = value;
        return this;
      }
      public Builder clearLzmaData() {
        result.hasLzmaData = false;
        result.lzmaData_ = getDefaultInstance().getLzmaData();
        return this;
      }
      
      // optional bytes bzip2_data = 5;
      public boolean hasBzip2Data() {
        return result.hasBzip2Data();
      }
      public com.google.protobuf.ByteString getBzip2Data() {
        return result.getBzip2Data();
      }
      public Builder setBzip2Data(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasBzip2Data = true;
        result.bzip2Data_ = value;
        return this;
      }
      public Builder clearBzip2Data() {
        result.hasBzip2Data = false;
        result.bzip2Data_ = getDefaultInstance().getBzip2Data();
        return this;
      }
      
      // @@protoc_insertion_point(builder_scope:Blob)
    }
    
    static {
      defaultInstance = new Blob(true);
      crosby.binary.Fileformat.internalForceInit();
      defaultInstance.initFields();
    }
    
    // @@protoc_insertion_point(class_scope:Blob)
  }
  
  public static final class BlockHeader extends
      com.google.protobuf.GeneratedMessage {
    // Use BlockHeader.newBuilder() to construct.
    private BlockHeader() {
      initFields();
    }
    private BlockHeader(boolean noInit) {}
    
    private static final BlockHeader defaultInstance;
    public static BlockHeader getDefaultInstance() {
      return defaultInstance;
    }
    
    @Override
	public BlockHeader getDefaultInstanceForType() {
      return defaultInstance;
    }
    
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return crosby.binary.Fileformat.internal_static_BlockHeader_descriptor;
    }
    
    @Override
	protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return crosby.binary.Fileformat.internal_static_BlockHeader_fieldAccessorTable;
    }
    
    // required string type = 1;
    public static final int TYPE_FIELD_NUMBER = 1;
    private boolean hasType;
    private java.lang.String type_ = "";
    public boolean hasType() { return hasType; }
    public java.lang.String getType() { return type_; }
    
    // optional bytes indexdata = 2;
    public static final int INDEXDATA_FIELD_NUMBER = 2;
    private boolean hasIndexdata;
    private com.google.protobuf.ByteString indexdata_ = com.google.protobuf.ByteString.EMPTY;
    public boolean hasIndexdata() { return hasIndexdata; }
    public com.google.protobuf.ByteString getIndexdata() { return indexdata_; }
    
    // required int32 datasize = 3;
    public static final int DATASIZE_FIELD_NUMBER = 3;
    private boolean hasDatasize;
    private int datasize_ = 0;
    public boolean hasDatasize() { return hasDatasize; }
    public int getDatasize() { return datasize_; }
    
    private void initFields() {
    }
    @Override
	public final boolean isInitialized() {
      if (!hasType) return false;
      if (!hasDatasize) return false;
      return true;
    }
    
    @Override
	public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (hasType()) {
        output.writeString(1, getType());
      }
      if (hasIndexdata()) {
        output.writeBytes(2, getIndexdata());
      }
      if (hasDatasize()) {
        output.writeInt32(3, getDatasize());
      }
      getUnknownFields().writeTo(output);
    }
    
    private int memoizedSerializedSize = -1;
    @Override
	public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;
    
      size = 0;
      if (hasType()) {
        size += com.google.protobuf.CodedOutputStream
          .computeStringSize(1, getType());
      }
      if (hasIndexdata()) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(2, getIndexdata());
      }
      if (hasDatasize()) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(3, getDatasize());
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }
    
    public static crosby.binary.Fileformat.BlockHeader parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static crosby.binary.Fileformat.BlockHeader parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static crosby.binary.Fileformat.BlockHeader parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static crosby.binary.Fileformat.BlockHeader parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static crosby.binary.Fileformat.BlockHeader parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static crosby.binary.Fileformat.BlockHeader parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    public static crosby.binary.Fileformat.BlockHeader parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static crosby.binary.Fileformat.BlockHeader parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static crosby.binary.Fileformat.BlockHeader parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static crosby.binary.Fileformat.BlockHeader parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    
    public static Builder newBuilder() { return Builder.create(); }
    @Override
	public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(crosby.binary.Fileformat.BlockHeader prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    @Override
	public Builder toBuilder() { return newBuilder(this); }
    
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> {
      private crosby.binary.Fileformat.BlockHeader result;
      
      // Construct using crosby.binary.Fileformat.BlockHeader.newBuilder()
      private Builder() {}
      
      private static Builder create() {
        Builder builder = new Builder();
        builder.result = new crosby.binary.Fileformat.BlockHeader();
        return builder;
      }
      
      @Override
	protected crosby.binary.Fileformat.BlockHeader internalGetResult() {
        return result;
      }
      
      @Override
	public Builder clear() {
        if (result == null) {
          throw new IllegalStateException(
            "Cannot call clear() after build().");
        }
        result = new crosby.binary.Fileformat.BlockHeader();
        return this;
      }
      
      @Override
	public Builder clone() {
        return create().mergeFrom(result);
      }
      
      @Override
	public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return crosby.binary.Fileformat.BlockHeader.getDescriptor();
      }
      
      @Override
	public crosby.binary.Fileformat.BlockHeader getDefaultInstanceForType() {
        return crosby.binary.Fileformat.BlockHeader.getDefaultInstance();
      }
      
      @Override
	public boolean isInitialized() {
        return result.isInitialized();
      }
      @Override
	public crosby.binary.Fileformat.BlockHeader build() {
        if (result != null && !isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return buildPartial();
      }
      
      private crosby.binary.Fileformat.BlockHeader buildParsed()
          throws com.google.protobuf.InvalidProtocolBufferException {
        if (!isInitialized()) {
          throw newUninitializedMessageException(
            result).asInvalidProtocolBufferException();
        }
        return buildPartial();
      }
      
      @Override
	public crosby.binary.Fileformat.BlockHeader buildPartial() {
        if (result == null) {
          throw new IllegalStateException(
            "build() has already been called on this Builder.");
        }
        crosby.binary.Fileformat.BlockHeader returnMe = result;
        result = null;
        return returnMe;
      }
      
      @Override
	public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof crosby.binary.Fileformat.BlockHeader) {
          return mergeFrom((crosby.binary.Fileformat.BlockHeader)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }
      
      public Builder mergeFrom(crosby.binary.Fileformat.BlockHeader other) {
        if (other == crosby.binary.Fileformat.BlockHeader.getDefaultInstance()) return this;
        if (other.hasType()) {
          setType(other.getType());
        }
        if (other.hasIndexdata()) {
          setIndexdata(other.getIndexdata());
        }
        if (other.hasDatasize()) {
          setDatasize(other.getDatasize());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }
      
      @Override
	public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder(
            this.getUnknownFields());
        while (true) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              this.setUnknownFields(unknownFields.build());
              return this;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                this.setUnknownFields(unknownFields.build());
                return this;
              }
              break;
            }
            case 10: {
              setType(input.readString());
              break;
            }
            case 18: {
              setIndexdata(input.readBytes());
              break;
            }
            case 24: {
              setDatasize(input.readInt32());
              break;
            }
          }
        }
      }
      
      
      // required string type = 1;
      public boolean hasType() {
        return result.hasType();
      }
      public java.lang.String getType() {
        return result.getType();
      }
      public Builder setType(java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasType = true;
        result.type_ = value;
        return this;
      }
      public Builder clearType() {
        result.hasType = false;
        result.type_ = getDefaultInstance().getType();
        return this;
      }
      
      // optional bytes indexdata = 2;
      public boolean hasIndexdata() {
        return result.hasIndexdata();
      }
      public com.google.protobuf.ByteString getIndexdata() {
        return result.getIndexdata();
      }
      public Builder setIndexdata(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasIndexdata = true;
        result.indexdata_ = value;
        return this;
      }
      public Builder clearIndexdata() {
        result.hasIndexdata = false;
        result.indexdata_ = getDefaultInstance().getIndexdata();
        return this;
      }
      
      // required int32 datasize = 3;
      public boolean hasDatasize() {
        return result.hasDatasize();
      }
      public int getDatasize() {
        return result.getDatasize();
      }
      public Builder setDatasize(int value) {
        result.hasDatasize = true;
        result.datasize_ = value;
        return this;
      }
      public Builder clearDatasize() {
        result.hasDatasize = false;
        result.datasize_ = 0;
        return this;
      }
      
      // @@protoc_insertion_point(builder_scope:BlockHeader)
    }
    
    static {
      defaultInstance = new BlockHeader(true);
      crosby.binary.Fileformat.internalForceInit();
      defaultInstance.initFields();
    }
    
    // @@protoc_insertion_point(class_scope:BlockHeader)
  }
  
  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_Blob_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_Blob_fieldAccessorTable;
  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_BlockHeader_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_BlockHeader_fieldAccessorTable;
  
  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\020fileformat.proto\"_\n\004Blob\022\013\n\003raw\030\001 \001(\014\022" +
      "\020\n\010raw_size\030\002 \001(\005\022\021\n\tzlib_data\030\003 \001(\014\022\021\n\t" +
      "lzma_data\030\004 \001(\014\022\022\n\nbzip2_data\030\005 \001(\014\"@\n\013B" +
      "lockHeader\022\014\n\004type\030\001 \002(\t\022\021\n\tindexdata\030\002 " +
      "\001(\014\022\020\n\010datasize\030\003 \002(\005B\017\n\rcrosby.binary"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        @Override
		public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_Blob_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_Blob_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_Blob_descriptor,
              new java.lang.String[] { "Raw", "RawSize", "ZlibData", "LzmaData", "Bzip2Data", },
              crosby.binary.Fileformat.Blob.class,
              crosby.binary.Fileformat.Blob.Builder.class);
          internal_static_BlockHeader_descriptor =
            getDescriptor().getMessageTypes().get(1);
          internal_static_BlockHeader_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_BlockHeader_descriptor,
              new java.lang.String[] { "Type", "Indexdata", "Datasize", },
              crosby.binary.Fileformat.BlockHeader.class,
              crosby.binary.Fileformat.BlockHeader.Builder.class);
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }
  
  public static void internalForceInit() {}
  
  // @@protoc_insertion_point(outer_class_scope)
}
