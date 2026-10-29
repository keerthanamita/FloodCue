<?php
declare(strict_types=1);
require_once __DIR__ . '/bootstrap.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') sendJson(405, ['ok'=>false,'error'=>'method_not_allowed']);
try {
    $uid = authenticateFirebaseUid(); $pdo = getDatabase();
    $token = rtrim(strtr(base64_encode(random_bytes(32)), '+/', '-_'), '=');
    $pdo->prepare('UPDATE emergency_qr_tokens SET revoked_at=NOW() WHERE owner_firebase_uid=:uid AND revoked_at IS NULL')->execute(['uid'=>$uid]);
    $pdo->prepare('INSERT INTO emergency_qr_tokens (token_hash,owner_firebase_uid,expires_at) VALUES (:hash,:uid,:expires)')->execute([
        'hash'=>hash('sha256',$token), 'uid'=>$uid, 'expires'=>(new DateTimeImmutable('+15 minutes'))->format('Y-m-d H:i:s')
    ]);
    sendJson(200, ['ok'=>true,'token'=>$token,'expires_at'=>(new DateTimeImmutable('+15 minutes'))->format('Y-m-d H:i:s')]);
} catch (Throwable $e) { error_log('FloodCue create_emergency_qr error: '.$e->getMessage()); sendJson(500,['ok'=>false,'error'=>'server_error']); }
