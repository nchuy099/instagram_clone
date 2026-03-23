import api from '../../../lib/axios';
import axios from 'axios';

export const mediaService = {
  getPresignedUrl: async (fileName: string, contentType: string): Promise<string> => {
    const response = await api.get('/media/presigned-url', {
      params: { fileName, contentType },
    });
    return response.data.data;
  },

  uploadFile: async (file: File): Promise<string> => {
    // 1. Get presigned URL
    const presignedUrl = await mediaService.getPresignedUrl(file.name, file.type);

    // 2. Upload to S3 directly using raw axios (without API base URL and interceptors)
    await axios.put(presignedUrl, file, {
      headers: {
        'Content-Type': file.type,
      },
      // Track progress if needed in the future
    });

    // 3. Return the clean URL (without query params)
    return presignedUrl.split('?')[0];
  },
};
