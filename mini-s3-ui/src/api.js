import axios from 'axios';
const api = axios.create({ baseURL: 'http://localhost:8080' });

export const createBucket = bucket =>
  api.post(`/buckets/${bucket}`);

export const listBuckets = () =>
  api.get('/buckets');

export const listObjects = bucket =>
  api.get(`/buckets/${bucket}/objects`);

export const uploadObject = (bucket, key, file) => {
  const form = new FormData();
  form.append('key', key);
  form.append('file', file);
  return api.post(`/buckets/${bucket}/objects`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

export const downloadObject = (bucket, key) =>
  api.get(`/buckets/${bucket}/objects/${encodeURIComponent(key)}`, {
    responseType: 'blob'
  });

export const deleteObject = (bucket, key) =>
  api.delete(`/buckets/${bucket}/objects/${encodeURIComponent(key)}`);

export const deleteBucket = (bucket) =>
  api.delete(`/buckets/${encodeURIComponent(bucket)}`);


