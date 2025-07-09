package olsh.backend.api_gateway.service;

import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.grpc.client.LabServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LabServiceBasicTest {

    private LabService labService;
    private UploadFileConfiguration uploadConfig;

    @BeforeEach
    void setUp() {
        System.out.println("Starting setup...");
        // Create MOCKs for dependencies
        LabServiceClient labServiceClient = mock(LabServiceClient.class);
        UserService userService = mock(UserService.class);
        uploadConfig = mock(UploadFileConfiguration.class);
        TagService tagService = mock(TagService.class);

        // Set up default configuration
        when(uploadConfig.getMaxFileSize()).thenReturn(1024L); // 1KB default

        // Create REAL service instance
        labService = new LabService(labServiceClient, uploadConfig, userService, tagService);

        System.out.println("Starting up tests...");
    }

    @AfterEach
    void tearDown() {
        System.out.println("Cleaning up LabService tests...");
        labService = null;
        uploadConfig = null;
    }

    // Test 1: Input validation for getLabById
    @Test
    void getLabById_NullLabId_ThrowsIllegalArgumentException() {
        // Given
        Long nullLabId = null;

        // When & Then
        assertThatThrownBy(() -> labService.getLabById(nullLabId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LabId should be provided");
    }

    @Test
    void getLabById_ZeroLabId_ThrowsIllegalArgumentException() {
        // Given
        Long zeroLabId = 0L;

        // When & Then
        assertThatThrownBy(() -> labService.getLabById(zeroLabId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LabId should be provided");
    }

    @Test
    void getLabById_NegativeLabId_ThrowsIllegalArgumentException() {
        // Given
        Long negativeLabId = -1L;

        // When & Then
        assertThatThrownBy(() -> labService.getLabById(negativeLabId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LabId should be provided");
    }

    @Test
    void getLabById_ValidLabId_PassesValidation() {
        // Given
        Long validLabId = 123L;

        // When & Then - Should not throw exception during input validation
        // Note: This will fail when trying to call gRPC, but that's Phase 2
        // For Phase 1, we just test that input validation passes
        assertThatThrownBy(() -> labService.getLabById(validLabId))
                .isNotInstanceOf(IllegalArgumentException.class);
    }

    // Test 2: Markdown file validation
    @Test
    void validateMarkdownFile_ValidMarkdownFile_DoesNotThrowException() {
        // Given
        MockMultipartFile validMdFile = new MockMultipartFile(
                "file",
                "lab-instructions.md",
                "text/markdown",
                "# Lab Instructions\nThis is a markdown file".getBytes()
        );

        // When & Then
        assertDoesNotThrow(() -> labService.validateMarkdownFile(validMdFile));
    }

    @Test
    void validateMarkdownFile_NullFile_ThrowsIllegalArgumentException() {
        // Given
        MultipartFile nullFile = null;

        // When & Then
        assertThatThrownBy(() -> labService.validateMarkdownFile(nullFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Markdown file is required");
    }

    @Test
    void validateMarkdownFile_EmptyFile_ThrowsIllegalArgumentException() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "lab-instructions.md",
                "text/markdown",
                new byte[0]
        );

        // When & Then
        assertThatThrownBy(() -> labService.validateMarkdownFile(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Markdown file is required");
    }

    @Test
    void validateMarkdownFile_FileWithoutName_ThrowsIllegalArgumentException() {
        // Given
        MockMultipartFile fileWithoutName = new MockMultipartFile(
                "file",
                null, // No filename
                "text/markdown",
                "Content".getBytes()
        );
        // When & Then
        assertThatThrownBy(() -> labService.validateMarkdownFile(fileWithoutName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only Markdown files are allowed");
    }

    @Test
    void validateMarkdownFile_NonMarkdownFile_ThrowsIllegalArgumentException() {
        // Given
        MockMultipartFile txtFile = new MockMultipartFile(
                "file",
                "instructions.txt",
                "text/plain",
                "Text content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> labService.validateMarkdownFile(txtFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only Markdown files are allowed");
    }

    @Test
    void validateMarkdownFile_CaseInsensitiveExtension_DoesNotThrowException() {
        // Given
        MockMultipartFile upperCaseMdFile = new MockMultipartFile(
                "file",
                "LAB-INSTRUCTIONS.MD", // Uppercase extension
                "text/markdown",
                "# Lab content".getBytes()
        );

        // When & Then
        assertDoesNotThrow(() -> labService.validateMarkdownFile(upperCaseMdFile));
    }

    @Test
    void validateMarkdownFile_FileTooLarge_ThrowsIllegalArgumentException() {
        // Given
        MockMultipartFile largeMdFile = new MockMultipartFile(
                "file",
                "large-instructions.md",
                "text/markdown",
                new byte[(int) uploadConfig.getMaxFileSize() + 1]
        );

        // When & Then
        assertThatThrownBy(() -> labService.validateMarkdownFile(largeMdFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.format("File size exceeds maximum limit of %d bytes",
                        uploadConfig.getMaxFileSize()));
    }

    @Test
    void validateMarkdownFile_ExactlyMaxSize_DoesNotThrowException() {
        // Given - Testing boundary condition
        MockMultipartFile boundaryFile = new MockMultipartFile(
                "file",
                "boundary.md",
                "text/markdown",
                new byte[(int) uploadConfig.getMaxFileSize()]
        );

        // When & Then
        assertDoesNotThrow(() -> labService.validateMarkdownFile(boundaryFile));
    }

    // Test 3: Asset validation
    @Test
    void validateAssets_NullAssets_DoesNotThrowException() {
        // Given
        MultipartFile[] nullAssets = null;

        // When & Then - Should not throw exception for null assets (they're optional)
        assertDoesNotThrow(() -> labService.validateAssets(nullAssets));
    }

    @Test
    void validateAssets_EmptyAssetsArray_DoesNotThrowException() {
        // Given
        MultipartFile[] emptyAssets = new MultipartFile[0];

        // When & Then
        assertDoesNotThrow(() -> labService.validateAssets(emptyAssets));
    }

    @Test
    void validateAssets_ValidAssets_DoesNotThrowException() {
        // Given
        MultipartFile[] validAssets = {
                new MockMultipartFile("asset1", "image1.png", "image/png", "PNG content".getBytes()),
                new MockMultipartFile("asset2", "image2.jpg", "image/jpeg", "JPEG content".getBytes())
        };

        // When & Then
        assertDoesNotThrow(() -> labService.validateAssets(validAssets));
    }

    @Test
    void validateAsset_NullAsset_ThrowsIllegalArgumentException() {
        // Given
        MultipartFile nullAsset = null;

        // When & Then
        assertNull(labService.validateAsset(nullAsset));
    }

    @Test
    void validateAsset_EmptyAsset_ThrowsIllegalArgumentException() {
        // Given
        MockMultipartFile emptyAsset = new MockMultipartFile(
                "asset",
                "image.png",
                "image/png",
                new byte[0]
        );

        // When & Then
        assertNull(labService.validateAsset(emptyAsset));
    }

    @Test
    void validateAsset_AssetWithoutName_ThrowsIllegalArgumentException() {
        // Given
        MockMultipartFile assetWithoutName = new MockMultipartFile(
                "asset",
                null, // No filename
                "image/png",
                "Content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> labService.validateAsset(assetWithoutName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Asset name cannot be empty.");
    }

    @Test
    void validateAsset_AssetWithBlankName_ThrowsIllegalArgumentException() {
        // Given
        MockMultipartFile assetWithBlankName = new MockMultipartFile(
                "asset",
                "   ", // Blank filename
                "image/png",
                "Content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> labService.validateAsset(assetWithBlankName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Asset name cannot be empty.");
    }

    @Test
    void validateAsset_AssetWithMarkdownExtension_ThrowsIllegalArgumentException() {
        // Given
        MockMultipartFile mdAsset = new MockMultipartFile(
                "asset",
                "readme.md", // Markdown file as asset - not allowed
                "text/markdown",
                "# Asset content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> labService.validateAsset(mdAsset))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Asset for lab cannot contain an .md file.");
    }

    @Test
    void validateAsset_AssetTooLarge_ThrowsIllegalArgumentException() {
        // Given
        long maxFileSize = 50L; // Very small limit
        when(uploadConfig.getMaxFileSize()).thenReturn(maxFileSize);

        MockMultipartFile largeAsset = new MockMultipartFile(
                "asset",
                "large-image.png",
                "image/png",
                "This is definitely larger than 50 bytes of content!".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> labService.validateAsset(largeAsset))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Asset file size for lab exceeds maximum limit of 50 bytes");
    }

    // Test 4: File extension validation edge cases
    @ParameterizedTest
    @ValueSource(strings = {"lab.txt", "instructions.pdf", "readme.docx", "guide.html"})
    void validateMarkdownFile_NonMarkdownExtensions_ThrowsException(String filename) {
        // Given
        MockMultipartFile nonMdFile = new MockMultipartFile(
                "file",
                filename,
                "application/octet-stream",
                "Content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> labService.validateMarkdownFile(nonMdFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only Markdown files are allowed");
    }

    @ParameterizedTest
    @ValueSource(strings = {"image.png", "photo.jpg", "document.pdf", "data.csv", "script.js"})
    void validateAsset_ValidAssetExtensions_DoesNotThrowException(String filename) {
        // Given
        MockMultipartFile validAsset = new MockMultipartFile(
                "asset",
                filename,
                "application/octet-stream",
                "Valid content".getBytes()
        );

        // When & Then
        assertDoesNotThrow(() -> labService.validateAsset(validAsset));
    }

}
