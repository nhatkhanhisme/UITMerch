import { supabase } from "./supabaseClient";

const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ALLOWED_MIME_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/gif",
  "image/webp",
  "image/svg+xml",
]);

const AVATAR_BUCKET =
  import.meta.env.VITE_SUPABASE_AVATAR_BUCKET?.trim() || "avatars";

const createFileId = () => {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }

  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

export async function uploadAvatarImage(file: File, userId: string) {
  if (!ALLOWED_MIME_TYPES.has(file.type)) {
    throw new Error("Please upload a JPG, PNG, GIF, SVG, or WEBP image.");
  }

  if (file.size > MAX_FILE_SIZE) {
    throw new Error("Image size must be 10MB or smaller.");
  }

  const extension = file.name.split(".").pop()?.toLowerCase() || "jpg";
  const filePath = `${userId}/${createFileId()}.${extension}`;

  const { error } = await supabase.storage
    .from(AVATAR_BUCKET)
    .upload(filePath, file, {
      contentType: file.type,
      upsert: false,
    });

  if (error) {
    throw new Error(error.message);
  }

  const { data } = supabase.storage.from(AVATAR_BUCKET).getPublicUrl(filePath);

  if (!data?.publicUrl) {
    throw new Error("Could not resolve the public URL for this upload.");
  }

  return data.publicUrl;
}
