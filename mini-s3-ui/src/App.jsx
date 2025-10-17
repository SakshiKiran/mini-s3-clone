import { useEffect, useState } from 'react';
import { createBucket, listBuckets, listObjects, uploadObject, downloadObject } from './api';

function App() {
  const [bucket, setBucket] = useState('');
  const [buckets, setBuckets] = useState([]);
  const [objects, setObjects] = useState([]);
  const [file, setFile] = useState(null);
  const [key, setKey] = useState('');

  const refresh = async () => {
    try {
      const bRes = await listBuckets();
      setBuckets(bRes.data);
      if (bucket) {
        const oRes = await listObjects(bucket);
        setObjects(oRes.data);
      } else {
        setObjects([]);
      }
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => { refresh(); }, [bucket]);

  const handleCreateBucket = async () => {
    await createBucket(bucket);
    await refresh();
    alert('Bucket created');
  };

  const handleUpload = async () => {
    await uploadObject(bucket, key || file.name, file);
    setKey('');
    setFile(null);
    await refresh();
    alert('Uploaded');
  };

  const handleDownload = async (k) => {
    const res = await downloadObject(bucket, k);
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const a = document.createElement('a');
    a.href = url;
    a.download = k.split('/').pop();
    a.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div style={{ padding: 16, fontFamily: 'sans-serif' }}>
      <h2>Mini S3 Clone</h2>

      <section>
        <h3>Create Bucket</h3>
        <input
          placeholder="Bucket name"
          value={bucket}
          onChange={e => setBucket(e.target.value)}
        />
        <button onClick={handleCreateBucket} disabled={!bucket}>
          Create Bucket
        </button>
      </section>

      <section style={{ marginTop: 24 }}>
        <h3>Upload File</h3>
        <input
          placeholder="Object key (optional)"
          value={key}
          onChange={e => setKey(e.target.value)}
        />
        <input
          type="file"
          onChange={e => setFile(e.target.files[0])}
        />
        <button
          onClick={handleUpload}
          disabled={!bucket || !file}
        >
          Upload
        </button>
      </section>

      <section style={{ marginTop: 24 }}>
        <h3>Buckets</h3>
        <ul>
          {buckets.map(b => (
            <li
              key={b}
              onClick={() => setBucket(b)}
              style={{
                cursor: 'pointer',
                textDecoration: bucket === b ? 'underline' : 'none'
              }}
            >
              {b}
            </li>
          ))}
        </ul>
      </section>

      <section style={{ marginTop: 24 }}>
        <h3>Objects in {bucket || '(select a bucket)'}</h3>
        <ul>
          {objects.map(o => (
            <li key={o.versionId + o.key}>
              {o.key} — {o.size} bytes — {new Date(o.uploadedAt).toLocaleString()}
              <button
                style={{ marginLeft: 8 }}
                onClick={() => handleDownload(o.key)}
              >
                Download
              </button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

export default App;
